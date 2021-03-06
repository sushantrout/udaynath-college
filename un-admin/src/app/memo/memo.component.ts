import { Component, OnInit } from '@angular/core';
import { MemoService } from '../services/memo.service';
import { MemoDetail, ReportDTO, MemoReportModel } from '../model/report-model';
import { ConfirmationService } from 'primeng/api';
import { NotificationService } from '../notification.service';

@Component({
  selector: 'app-memo',
  templateUrl: './memo.component.html',
  styleUrls: ['./memo.component.css']
})
export class MemoComponent implements OnInit {

  constructor(private memoService: MemoService,
    private confirmationService : ConfirmationService,
    private notificationService : NotificationService) { }
  memoDetails: MemoReportModel = new MemoReportModel();
  selectedMemo: MemoDetail = new MemoDetail();
  displayAbsentList = false;
  searchText = "";
  presentStudentStatus = false;
  ngOnInit(): void {
    this.memoDetails = new MemoReportModel();;
    this.selectedMemo = new MemoDetail();
    this.displayAbsentList = false;
    this.presentStudentStatus = false;
    this.searchText = "";
    this.findAll();
  }
  findAll() {
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

  deleteMemoData() {
    let self = this;
    this.confirmationService.confirm({
      message: `Are you sure you want to delete <b> ${self.selectedMemo.name} </b>?`,
      accept: () => {
        self.memoService.delete(self.selectedMemo.name).subscribe(res => {
          self.notificationService.showSuccess('Successfully deleted', 'Memo');
          this.selectedMemo = new MemoDetail();
          this.findAll();
        });
      }
    });
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
  getColor(st){
    let color = st.studentPresent ? '': '#99b1d2';
    return color;
  }
  itemRools = [];
  selectedRoles = [];
  showDialog(itemRools){
    this.searchText = "";
    this.presentStudentStatus = false;
    this.displayAbsentList = true;
    this.selectedRoles = itemRools;
    this.itemRools = JSON.parse(JSON.stringify(itemRools));
  }
  saveChanged(){
    //this.selectedRoles = JSON.parse(JSON.stringify(this.itemRools));
    this.cancel();
  }
  cancel(){
    this.displayAbsentList = false;
  }
}
