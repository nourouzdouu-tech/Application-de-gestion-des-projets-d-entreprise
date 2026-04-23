import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Conversation {
  id: number;
  otherParticipantId: number;
  otherParticipantName: string;
  lastMessage: string;
  lastMessageTime: string;
  unreadCount: number;
}

export interface MessageReaction {
  emoji: string;
  userId: number;
  userName: string;
}

export interface MessageAttachment {
  id?: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  fileUrl: string;
}

export interface Message {
  id: number;
  content: string;
  senderId: number;
  senderName: string;
  receiverId: number;
  receiverName: string;
  sentAt: string;
  read: boolean;
  clientTempId?: string;
  replyToMessageId?: number | null;

  fileName?: string | null;
  fileType?: string | null;
  fileSize?: number | null;
  fileUrl?: string | null;

  reactions?: MessageReaction[];
  attachments?: MessageAttachment[];
}

@Injectable({ providedIn: 'root' })
export class MessageService {
  private readonly apiUrl = 'http://localhost:8080/api/messages';

  constructor(private http: HttpClient) {}

  getConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(`${this.apiUrl}/conversations`);
  }

  getMessages(conversationId: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.apiUrl}/conversation/${conversationId}/messages`);
  }

  sendMessage(
    receiverId: number,
    content: string,
    clientTempId?: string,
    replyToMessageId?: number | null
  ): Observable<Message> {
    return this.http.post<Message>(`${this.apiUrl}/send/${receiverId}`, {
      content,
      clientTempId: clientTempId ?? null,
      replyToMessageId: replyToMessageId ?? null
    });
  }

  sendFile(
    receiverId: number,
    file: File,
    clientTempId?: string,
    replyToMessageId?: number | null
  ): Observable<Message> {
    const formData = new FormData();
    formData.append('file', file);

    if (clientTempId) {
      formData.append('clientTempId', clientTempId);
    }

    if (replyToMessageId != null) {
      formData.append('replyToMessageId', String(replyToMessageId));
    }

    return this.http.post<Message>(`${this.apiUrl}/send-file/${receiverId}`, formData);
  }

  getOrCreateConversation(otherUserId: number): Observable<Conversation> {
    return this.http.get<Conversation>(`${this.apiUrl}/conversation/${otherUserId}`);
  }
}