import { Component, OnInit } from '@angular/core';
import { MemoService } from '../services/memo.service';
import { MemoDetail, ReportDTO, MemoReportModel } from '../model/report-model';

@Component({
  selector: 'app-memo',
  templateUrl: './memo.component.html',
  styleUrls: ['./memo.component.css']
})
export class MemoComponent implements OnInit {

  constructor(private memoService: MemoService) { }
  memoDetails: MemoReportModel = new MemoReportModel();
  selectedMemo: MemoDetail = new MemoDetail();
  ngOnInit(): void {
    this.memoDetails = new MemoReportModel();;
    this.selectedMemo = new MemoDetail();
    this.memoService.getAll().subscribe((res: MemoReportModel) => {
      this.memoDetails = res;
    });
  }
  getNameForHeader(item) {
    let text = "";

    if (item.memoRollNumberList) {

      let itemAllRoll = [];
      for (let itemRolls of item.memoRollNumberList) {
        for (let roll of itemRolls) {
          itemAllRoll.push(roll);
        }
      }

      let presentLength = itemAllRoll.filter(function (item) {
        return item.studentPresent;
      }).length;

      text = text + " P:" + presentLength + " ,";
      let absentLength = itemAllRoll.filter(function (item) {
        return !item.studentPresent;
      }).length;
      text = text + " A:" + absentLength;
    }
    return item.name + "  ( "+text+ " )";
  }

  export(){
    this.memoService.export(this.selectedMemo).subscribe(response => {
        var newBlob = new Blob([response.body]);
        if (window.navigator && window.navigator.msSaveOrOpenBlob) {
          window.navigator.msSaveOrOpenBlob(newBlob);
          return;
        }
  
        var link = document.createElement('a');
        const data = window.URL.createObjectURL(newBlob);
        link.setAttribute("href", data);
        link.download = this.selectedMemo.name+ ".zip";
  
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(data);
    });
  }
}
