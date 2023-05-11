import { Injectable } from '@angular/core';

export interface Notification {
  id?: number;
  type?: NotificationType;
  title?: string;
  data?: any;
  close?: (notifications: Notification[]) => void;
}

export enum NotificationType {
  COMPONENT = 'COMPONENT',
  LICENSE = 'LICENSE',
  TEXT = 'TEXT',
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  position = 'bottom right';

  // unique id for each notification. Starts from 0.
  private notificationId = 0;
  private notifications: Notification[] = [];

  clear(): void {
    this.notifications = [];
  }

  get(): Notification[] {
    return this.notifications;
  }

  /**
   * Adds notification to notifications array and returns added notification.
   * @param notification      Notification to add.
   * @param extNotifications  If missing then adding `notification` to `NotificationService` internal array and notifications can be retrieved by `get()`.
   *                   Else adding `notification` to `extNotifications`.
   * @returns  Added notification
   */
  addNotification(notification: Notification, extNotifications?: Notification[]): Notification {
    notification.id = this.notificationId++;
    notification.close = (notificationsArray: Notification[]) => this.closeNotification(notification.id!, notificationsArray);
    (extNotifications ?? this.notifications).push(notification);

    return notification;
  }

  private closeNotification(notificationId: number, extNotifications?: Notification[]): void {
    const notifications = extNotifications ?? this.notifications;
    const notificationIndex = notifications.map(notification => notification.id).indexOf(notificationId);
    // if found notification then remove
    if (notificationIndex >= 0) {
      notifications.splice(notificationIndex, 1);
    }
  }
}
