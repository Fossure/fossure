import { Component, OnInit, OnDestroy } from '@angular/core';
import { NotificationService, Notification, NotificationType } from 'app/core/util/notification.service';

@Component({
  selector: 'jhi-notification',
  templateUrl: './notification.component.html',
})
export class NotificationComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  notificationType = NotificationType;

  constructor(private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.notifications = this.notificationService.get();
  }

  ngOnDestroy(): void {
    this.notificationService.clear();
  }

  close(notification: Notification): void {
    notification.close?.(this.notifications);
  }
}
