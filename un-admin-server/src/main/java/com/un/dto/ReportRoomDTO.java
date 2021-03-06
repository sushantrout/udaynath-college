package com.un.dto;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class ReportRoomDTO {
	String deptId;
	String deptName;
	String id;
	String name;
	String noOfRow;
	List<List<String>> rollNumberList;
	List<List<MemoRollNumber>> memoRollNumberList;
	String rowCapacity;
	String title;
	Date date;
	String start;
	String end;
	String checked;
}
