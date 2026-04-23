import { Injectable } from '@angular/core';
import { Client, Message as StompMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';
import { Subject, Observable } from 'rxjs';

export interface WebSocketMessage {
  type?: 'message' | 'reaction';

  id?: number;
  clientTempId?: string;
  content?: string;

  senderId: number;
  senderName: string;

  receiverId: number;
  receiverName?: string;

  sentAt?: string;
  read?: boolean;

  replyToMessageId?: number | null;

  messageId?: number;
  emoji?: string;
  isAdd?: boolean;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private stompClient: Client | null = null;
  private messageSubject = new Subject<WebSocketMessage>();

  constructor(private authService: AuthService) {}

  connect(): void {
    if (this.stompClient?.active || this.stompClient?.connected) {
      return;
    }

    const token = this.authService.getToken();
    if (!token) {
      console.error('❌ Pas de token JWT');
      return;
    }

    const socket = new SockJS('http://localhost:8080/ws-messages');

    this.stompClient = new Client({
      webSocketFactory: () => socket,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      debug: (str) => console.log('[STOMP]', str)
    });

    this.stompClient.onConnect = () => {
      console.log('✅ WebSocket connecté');

      // IMPORTANT: côté client Spring STOMP utilise /user/queue/messages
      this.stompClient?.subscribe('/user/queue/messages', (message: StompMessage) => {
        try {
          const parsed = JSON.parse(message.body) as WebSocketMessage;
          console.log('📩 WS reçu:', parsed);
          this.messageSubject.next(parsed);
        } catch (error) {
          console.error('Erreur parsing message:', error);
        }
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('❌ Erreur STOMP:', frame);
    };

    this.stompClient.onWebSocketClose = () => {
      console.warn('⚠️ WebSocket fermé');
    };

    this.stompClient.onDisconnect = () => {
      console.warn('⚠️ WebSocket déconnecté');
    };

    this.stompClient.activate();
  }

  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }

  sendMessage(
    receiverId: number,
    content: string,
    clientTempId?: string,
    messageId?: number,
    replyToMessageId?: number | null
  ): void {
    const user = this.authService.getUser();

    if (!user || !this.stompClient?.connected) {
      console.error('WebSocket non connecté ou utilisateur non trouvé');
      return;
    }

    const message: WebSocketMessage = {
      type: 'message',
      id: messageId,
      clientTempId,
      content,
      receiverId,
      senderId: user.id ?? 0,
      senderName: `${user.prenom} ${user.nom}`.trim(),
      receiverName: '',
      sentAt: new Date().toISOString(),
      read: false,
      replyToMessageId: replyToMessageId ?? null
    };

    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(message)
    });
  }

  sendReaction(receiverId: number, messageId: number, emoji: string, isAdd: boolean): void {
    const user = this.authService.getUser();

    console.log('=== FRONT sendReaction ===');
    console.log({ receiverId, messageId, emoji, isAdd, connected: this.stompClient?.connected, user });

    if (!user || !this.stompClient?.connected) {
      console.error('WebSocket non connecté ou utilisateur non trouvé');
      return;
    }

    const reaction: WebSocketMessage = {
      type: 'reaction',
      messageId,
      emoji,
      isAdd,
      receiverId,
      senderId: user.id ?? 0,
      senderName: `${user.prenom} ${user.nom}`.trim(),
      receiverName: '',
      sentAt: new Date().toISOString()
    };

    this.stompClient.publish({
      destination: '/app/chat.reaction',
      body: JSON.stringify(reaction)
    });

    console.log('=== FRONT publish /app/chat.reaction OK ===');
  }

  getMessages(): Observable<WebSocketMessage> {
    return this.messageSubject.asObservable();
  }
}