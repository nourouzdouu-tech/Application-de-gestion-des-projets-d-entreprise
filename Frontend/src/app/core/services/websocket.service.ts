import { Injectable } from '@angular/core';
import { Client, Message as StompMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from './auth.service';
import { Subject } from 'rxjs';

export interface ChatMessage {
  id?: number;
  content: string;
  senderId: number;
  senderName: string;
  receiverId: number;
  receiverName?: string;
  sentAt?: string;
  read?: boolean;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private stompClient: Client | null = null;
  private messageSubject = new Subject<ChatMessage>();

  constructor(private authService: AuthService) {}

  connect(): void {
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
      debug: (str) => console.log('[STOMP]', str)  // ✅ log STOMP
    });

    this.stompClient.onConnect = () => {
      console.log('✅ WebSocket connecté');
      const user = this.authService.getUser();
      console.log('👤 User connecté:', user);

      const userEmail = user?.email;
      if (userEmail) {
        const destination = `/user/${userEmail}/queue/messages`;
        console.log('📡 Abonnement à:', destination);

        this.stompClient?.subscribe(destination, (message: StompMessage) => {
          console.log('📨 Message brut reçu:', message.body);
          const msg: ChatMessage = JSON.parse(message.body);
          console.log('📨 Message parsé:', msg);
          this.messageSubject.next(msg);
        });
      } else {
        console.error('❌ Email utilisateur introuvable, abonnement impossible');
      }
    };

    this.stompClient.onStompError = (frame) => {
      console.error('❌ Erreur STOMP:', frame);
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

  sendMessage(receiverId: number, content: string): void {
    const user = this.authService.getUser();
    if (!user || !this.stompClient?.connected) {
        console.error('WebSocket non connecté ou utilisateur non trouvé');
        return;
    }

    const message: ChatMessage = {
        content,
        receiverId,
        senderId: 0, // valeur factice (non utilisée par le backend)
        senderName: `${user.prenom} ${user.nom}`
    };
    this.stompClient.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(message)
    });
}
getMessages(): Subject<ChatMessage> {
  return this.messageSubject;
}
}