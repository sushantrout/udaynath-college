package com.un.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.hibernate.engine.jdbc.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.un.dto.MemoDTO;
import com.un.dto.MemoReportDTO;
import com.un.dto.MemoRollNumber;
import com.un.dto.ReportDTO;
import com.un.dto.ReportRoomDTO;
import com.un.dto.RollNumberForSeatArrangement;
import com.un.service.MemoService;
import com.un.util.DocsUtil;
import com.un.util.MemoPatternGenrater;

@RestController
@RequestMapping(value = "api/memo")
public class MemoResource {
	@Autowired
	private MemoService memoService;

	@GetMapping
	public MemoReportDTO getAllMemoList() {
		List<MemoDTO> memoNameList = memoService.getMemoNameList();
		for (MemoDTO memoDTO : memoNameList) {
			ReportDTO reportDTO = memoDTO.getReportDTO();
			List<ReportRoomDTO> selectedRooms = reportDTO.getSelectedRooms();
			for (ReportRoomDTO roomDTO : selectedRooms) {
				List<List<String>> rollNumberList = roomDTO.getRollNumberList();
				List<List<MemoRollNumber>> memoRollNumbersList = new ArrayList<>();
				if(rollNumberList == null) {
					continue;
				}
				for (List<String> rollNumbers : rollNumberList) {
					List<MemoRollNumber> memoRollNumbers = new ArrayList<>();

					for (String roll : rollNumbers) {
						MemoRollNumber memoRollNumber = new MemoRollNumber();
						memoRollNumber.setRollMumber(roll);
						memoRollNumber.setStudentPresent(true);
						memoRollNumbers.add(memoRollNumber);
					}
					memoRollNumbersList.add(memoRollNumbers);
				}
				roomDTO.setMemoRollNumberList(memoRollNumbersList);
			}
		}
		MemoReportDTO reportDTO = new MemoReportDTO();
		reportDTO.setMemoModels(memoNameList);
		return reportDTO;
	}

	@PostMapping(produces = "application/zip")
	public void downloadmemoDetails(HttpServletResponse response, @RequestBody MemoDTO memoDTO) throws IOException {
		Boolean isElective = memoDTO.getReportDTO().getElective();
		isElective = isElective == null ? false : isElective;
		ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());

		ReportDTO reportDTO = memoDTO.getReportDTO();
		List<ReportRoomDTO> selectedRooms = reportDTO.getSelectedRooms();

		List<MemoRollNumber> allStudent = new ArrayList<>();

		for (ReportRoomDTO reportRoomDTO : selectedRooms) {
			List<List<MemoRollNumber>> memoRollNumberList = reportRoomDTO.getMemoRollNumberList();
			if (memoRollNumberList != null) {
				allStudent.addAll(memoRollNumberList.stream().filter(p -> p != null).flatMap(a -> a.stream())
						.collect(Collectors.toList()));
			}
		}

		List<MemoRollNumber> rollNumberList = allStudent.stream()
				.filter(p -> p.getRollMumber() != null && !p.getRollMumber().isEmpty()).collect(Collectors.toList());

		List<RollNumberForSeatArrangement> seatArrangements = new ArrayList<>();
		for (MemoRollNumber memoRoll : rollNumberList) {
			String data = memoRoll.getRollMumber();
			RollNumberForSeatArrangement roll = new RollNumberForSeatArrangement();
			int length = data.length();
			String prefix = data.substring(0, length - reportDTO.getRollNumberLength());
			roll.setPrefix(prefix);
			roll.setRollNumber(Integer.valueOf(data.substring(length - reportDTO.getRollNumberLength())));
			roll.setPresent(memoRoll.isStudentPresent());
			seatArrangements.add(roll);
		}

		Map<String, List<RollNumberForSeatArrangement>> rollNumberGroup = seatArrangements.stream()
				.collect(Collectors.groupingBy(RollNumberForSeatArrangement::getPrefix));

		Set<String> allStream = rollNumberGroup.keySet();
		if(isElective) {
			List<String> allMemoString = new ArrayList<>();
			allMemoString.add("");
			allMemoString.add("");
			long totalCount = 0;
			String dateOfExam = reportDTO.getDateOfExam();
			for (String stream : allStream) {
				List<RollNumberForSeatArrangement> seats = rollNumberGroup.get(stream);
				seats = seats.stream().filter(p -> p != null).filter(p -> p.getRollNumber() != null)
						.sorted(Comparator.comparing(RollNumberForSeatArrangement::getRollNumber))
						.collect(Collectors.toList());
				List<String> memoString = MemoPatternGenrater.getMemoString(seats, stream);
				
				List<List<RollNumberForSeatArrangement>> presentStudentList = new ArrayList<>();
				List<RollNumberForSeatArrangement> pStudents = new ArrayList<>();
				List<Integer> absentStudent = new ArrayList<>();
				int index = 0;
				for (RollNumberForSeatArrangement seat : seats) {
					index++;
					if (seat.isPresent()) {
						pStudents.add(seat);
						if(index == seats.size()) {
							presentStudentList.add(pStudents);
						}
					} else {
						if(!pStudents.isEmpty()) {
							presentStudentList.add(pStudents);					
						}
						pStudents = new ArrayList<>();
						absentStudent.add(seat.getRollNumber());
					}
				}
				long count = presentStudentList.stream().flatMap(d -> d.stream()).count();
				
				allMemoString.set(0, allMemoString.get(0) + memoString.get(0) + "--");
				if(memoString.size() == 2) {
					allMemoString.set(1, allMemoString.get(1) + memoString.get(1) + "--");					
				}
				
				totalCount = totalCount + count;
			}
			generateReport(allMemoString, "Elective", zipOut, reportDTO.getExamName(), dateOfExam, totalCount);
		} else {
			for (String stream : allStream) {
				List<RollNumberForSeatArrangement> seats = rollNumberGroup.get(stream);
				seats = seats.stream().filter(p -> p != null).filter(p -> p.getRollNumber() != null)
						.sorted(Comparator.comparing(RollNumberForSeatArrangement::getRollNumber))
						.collect(Collectors.toList());
				List<String> memoString = MemoPatternGenrater.getMemoString(seats, stream);
				
				List<List<RollNumberForSeatArrangement>> presentStudentList = new ArrayList<>();
				List<RollNumberForSeatArrangement> pStudents = new ArrayList<>();
				List<Integer> absentStudent = new ArrayList<>();
				int index = 0;
				for (RollNumberForSeatArrangement seat : seats) {
					index++;
					if (seat.isPresent()) {
						pStudents.add(seat);
						if(index == seats.size()) {
							presentStudentList.add(pStudents);
						}
					} else {
						if(!pStudents.isEmpty()) {
							presentStudentList.add(pStudents);					
						}
						pStudents = new ArrayList<>();
						absentStudent.add(seat.getRollNumber());
					}
				}
				long count = presentStudentList.stream().flatMap(d -> d.stream()).count();
				
				String dateOfExam = reportDTO.getDateOfExam();
				generateReport(memoString, stream, zipOut, reportDTO.getExamName(), dateOfExam, count);
			}
		}
		

	}

	private void generateReport(List<String> memoString, String name, ZipOutputStream zipOut, String examName, String dateOfExam, long presentCount) {
		String file = this.getClass().getResource("/static/Memo.docx").getFile();
		try {
			InputStream input = new FileInputStream(file);
			XWPFDocument document = new XWPFDocument(input);
			
			List<XWPFParagraph> paragraphs = document.getParagraphs();
			
			DocsUtil.replaceParagraph(paragraphs.get(11), "EXAMNAMETOREPLACE", examName);
			DocsUtil.replaceParagraph(paragraphs.get(15), "REPLACEDATE", dateOfExam);
			DocsUtil.replaceParagraph(paragraphs.get(16), "161", ""+presentCount);
			List<XWPFTable> tables = document.getTables();

			for (XWPFTable table : tables) {
				String[] split = memoString.get(0).split("--");
				int rowIndex = 0;
				for(String text : split) {
					XWPFTableRow row = table.getRow(rowIndex);
					if(row == null) {
						row = table.createRow();
					}
					XWPFTableCell cell = row.getCell(0);
					if(cell == null) {
						cell = row.createCell();
					}
					cell.setText(text);
					rowIndex++;
				}
			}

			XWPFParagraph xwpfParagraph = paragraphs.get(24);
			if(memoString.size() >= 2) {
				String[] strings = memoString.get(1).split("--");
				XWPFRun dataRun = xwpfParagraph.createRun();
				dataRun.setText("C. Absentee Roll Nos.  ");
				dataRun.addBreak();
				for(String text : strings) {
					dataRun = xwpfParagraph.createRun();
					dataRun.setText("\t\t"+text);
					dataRun.addBreak();
				}
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.write(out);
			document.close();
			out.close();

			byte[] responseByteArray = out.toByteArray();
			ZipEntry zipEntry = new ZipEntry(name + "- memo" + ".docx");
			zipEntry.setSize(responseByteArray.length);
			zipOut.putNextEntry(zipEntry);
			InputStream inputStream = new ByteArrayInputStream(responseByteArray);
			StreamUtils.copy(inputStream, zipOut);
			zipOut.closeEntry();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@DeleteMapping(value = "/{examName}")
	public void delete(@PathVariable String examName) {
		memoService.delete(examName);
	}
}
