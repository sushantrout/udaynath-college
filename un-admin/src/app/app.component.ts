import { Component } from '@angular/core';
import { NotificationService } from './notification.service'
import {MenuItem} from 'primeng/api';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'un-admin';
  constructor() {
  }
  items: MenuItem[];

  ngOnInit() {
    this.items = [
        {label: 'Home', icon: 'pi pi-fw pi-home', routerLink: ['home']},
        {label: 'Block', icon: 'pi pi-fw pi-table', routerLink: ['department']},
        {label: 'Room', icon: 'pi pi-fw pi-ticket', routerLink: ['assign-room']},
        {label: 'Enrollment', icon: 'pi pi-fw pi-cog', routerLink: ['enroll-room']},
        {label: 'Memo', icon: 'pi pi-fw pi-wallet', routerLink: ['memo']},
    ];
}
}
