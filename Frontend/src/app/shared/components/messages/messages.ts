import {
  Component,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild,
  ChangeDetectorRef,
  HostListener
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import {
  MessageService,
  Conversation,
  Message,
  MessageAttachment
} from '../../../core/services/message.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { WebSocketService, WebSocketMessage } from '../../../core/services/websocket.service';
import { Subscription } from 'rxjs';

type LocalMessage = Message & {
  pending?: boolean;
  clientTempId?: string;
  error?: boolean;
  uploadProgress?: number;
};

@Component({
  selector: 'app-shared-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messages.html',
  styleUrls: ['./messages.css']
})
export class SharedMessagesComponent implements OnInit, OnDestroy {
  conversations: Conversation[] = [];
  selectedConv: Conversation | null = null;
  messages: LocalMessage[] = [];
  newMessage = '';
  currentUserId = 0;
  isSending = false;

  showNewMessageModal = false;
  newMessageReceiverId: number | null = null;
  usersList: User[] = [];

  replyingToMessage: LocalMessage | null = null;
  selectedFiles: MessageAttachment[] = [];

  lightboxUrl: string | null = null;

  showEmojiPicker = false;
  emojiList = ['😊', '😂', '❤️', '👍', '🎉', '🔥', '😍', '🙏', '😢', '😎', '👏', '🤔', '😅', '💪', '✅', '🫡', '😆', '🥳', '👀', '💯'];

  readonly maxFileSizeBytes = 1024 * 1024; // 1 MB

  private wsSubscription?: Subscription;

  @ViewChild('chatMessages') private chatMessagesContainer!: ElementRef;
  @ViewChild('messageTextarea') private messageTextareaRef!: ElementRef<HTMLTextAreaElement>;

  constructor(
    private messageService: MessageService,
    private authService: AuthService,
    private userService: UserService,
    private wsService: WebSocketService,
    private cdr: ChangeDetectorRef,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    const user = this.authService.getUser();
    this.currentUserId = user?.id ?? 0;

    this.wsService.connect();

    this.wsSubscription = this.wsService.getMessages().subscribe((wsMsg: WebSocketMessage) => {
      if (!wsMsg) return;

      if (wsMsg.type === 'reaction') {
        this.handleReactionMessage(wsMsg);
        return;
      }

      if (wsMsg.type === 'file') {
        this.handleFileMessage(wsMsg);
        return;
      }

      const incomingMessage: LocalMessage = {
        id: wsMsg.id ?? wsMsg.messageId ?? Date.now(),
        clientTempId: wsMsg.clientTempId,
        content: wsMsg.content || '',
        senderId: wsMsg.senderId,
        senderName: wsMsg.senderName,
        receiverId: wsMsg.receiverId,
        receiverName: wsMsg.receiverName || '',
        sentAt: wsMsg.sentAt || new Date().toISOString(),
        read: wsMsg.read || false,
        replyToMessageId: wsMsg.replyToMessageId ?? null,
        reactions: [],
        attachments: [],
        pending: false
      };

      this.handleIncomingMessage(incomingMessage);
    });

    this.loadConversations();
    this.loadUsersList();
  }

  ngOnDestroy(): void {
    this.wsSubscription?.unsubscribe();
    this.wsService.disconnect();
  }

  renderMarkdown(text: string): SafeHtml {
    if (!text) return this.sanitizer.bypassSecurityTrustHtml('');

    let html = this.escapeHtml(text);

    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/(?<![_*])_([^_]+)_(?![_*])/g, '<em>$1</em>');
    html = html.replace(/(?<!\*)\*(?!\*)([^*]+)(?<!\*)\*(?!\*)/g, '<em>$1</em>');
    html = html.replace(
      /\[([^\]]+)\]\((https?:\/\/[^)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer" class="msg-link">$1</a>'
    );

    const lines = html.split('\n');
    let inList = false;
    const result: string[] = [];

    for (const line of lines) {
      if (line.startsWith('- ')) {
        if (!inList) {
          result.push('<ul class="msg-list">');
          inList = true;
        }
        result.push(`<li>${line.substring(2)}</li>`);
      } else {
        if (inList) {
          result.push('</ul>');
          inList = false;
        }
        result.push(line);
      }
    }

    if (inList) result.push('</ul>');
    html = result.join('\n');
    html = html.replace(/\n/g, '<br>');

    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  toggleEmojiPicker(): void {
    this.showEmojiPicker = !this.showEmojiPicker;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.emoji-picker-wrapper')) {
      this.showEmojiPicker = false;
    }
  }

  insertEmoji(emoji: string): void {
    const textarea = this.messageTextareaRef?.nativeElement;

    if (textarea) {
      const start = textarea.selectionStart ?? this.newMessage.length;
      const end = textarea.selectionEnd ?? this.newMessage.length;
      this.newMessage =
        this.newMessage.substring(0, start) + emoji + this.newMessage.substring(end);

      setTimeout(() => {
        textarea.focus();
        const pos = start + emoji.length;
        textarea.selectionStart = pos;
        textarea.selectionEnd = pos;
      }, 0);
    } else {
      this.newMessage += emoji;
    }

    this.showEmojiPicker = false;
    this.cdr.detectChanges();
  }

  applyFormat(type: 'bold' | 'italic' | 'link' | 'list'): void {
    const textarea = this.messageTextareaRef?.nativeElement;
    if (!textarea) return;

    const start = textarea.selectionStart ?? 0;
    const end = textarea.selectionEnd ?? 0;
    const selected = this.newMessage.substring(start, end);

    let replacement = '';
    let cursorStart = start;
    let cursorEnd = start;

    switch (type) {
      case 'bold':
        replacement = `**${selected || 'texte en gras'}**`;
        cursorStart = selected ? start + replacement.length : start + 2;
        cursorEnd = selected ? cursorStart : start + 2 + 14;
        break;
      case 'italic':
        replacement = `_${selected || 'texte en italique'}_`;
        cursorStart = selected ? start + replacement.length : start + 1;
        cursorEnd = selected ? cursorStart : start + 1 + 17;
        break;
      case 'link':
        replacement = `[${selected || 'texte'}](https://url.com)`;
        cursorStart = start + replacement.indexOf('https://');
        cursorEnd = start + replacement.length - 1;
        break;
      case 'list':
        const prefix = (start === 0 || this.newMessage[start - 1] === '\n') ? '' : '\n';
        replacement = `${prefix}- ${selected || 'élément'}`;
        cursorStart = start + replacement.length;
        cursorEnd = cursorStart;
        break;
    }

    this.newMessage =
      this.newMessage.substring(0, start) +
      replacement +
      this.newMessage.substring(end);

    setTimeout(() => {
      textarea.focus();
      textarea.selectionStart = cursorStart;
      textarea.selectionEnd = cursorEnd;
      this.autoResizeTextarea(textarea);
    }, 0);

    this.cdr.detectChanges();
  }

  loadConversations(): void {
    this.messageService.getConversations().subscribe({
      next: (data: Conversation[]) => {
        this.conversations = [...data].sort(
          (a, b) => new Date(b.lastMessageTime).getTime() - new Date(a.lastMessageTime).getTime()
        );

        if (this.selectedConv) {
          const selected = this.conversations.find(
            c => c.otherParticipantId === this.selectedConv!.otherParticipantId
          );
          if (selected) {
            this.selectedConv = { ...selected, unreadCount: 0 };
          }
        }

        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur chargement conversations', err)
    });
  }

  loadUsersList(): void {
    this.userService.getAllUsers().subscribe({
      next: (users: User[]) => {
        this.usersList = users.filter(u => u.id !== this.currentUserId);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur chargement utilisateurs', err);
        this.usersList = [];
        this.cdr.detectChanges();
      }
    });
  }

  selectConversation(conv: Conversation): void {
    if (conv.id < 0) {
      this.messageService.getOrCreateConversation(conv.otherParticipantId).subscribe({
        next: (realConv) => {
          this.conversations = this.conversations.map(c =>
            c.otherParticipantId === conv.otherParticipantId
              ? { ...realConv, unreadCount: 0 }
              : c
          );
          this.sortConversations();
          this.openConversation(realConv);
        },
        error: (err) => console.error('Erreur résolution conversation', err)
      });
      return;
    }

    this.openConversation(conv);
  }

  private openConversation(conv: Conversation): void {
    this.selectedConv = { ...conv, unreadCount: 0 };
    this.replyingToMessage = null;
    this.selectedFiles = [];

    this.conversations = this.conversations.map(c =>
      c.id === conv.id ? { ...c, unreadCount: 0 } : c
    );

    this.messageService.getMessages(conv.id).subscribe({
      next: (msgs: Message[]) => {
        // Charger messageslocaux par conversation ID ET par participants
        const localMessagesById = this.loadLocalConversationMessages(conv.id);
        const localMessagesByParticipants = this.loadLocalConversationMessagesByParticipants(conv.otherParticipantId);
        
        // Fusionner les deux sources locales
        const combinedLocal = [...localMessagesById];
        for (const msg of localMessagesByParticipants) {
          const exists = combinedLocal.some(m =>
            m.id === msg.id ||
            (!!m.clientTempId && !!msg.clientTempId && m.clientTempId === msg.clientTempId)
          );
          if (!exists) {
            combinedLocal.push(msg);
          }
        }

        const serverMessages = msgs.map(msg =>
          this.applyLocalMetaToSingleMessage(this.normalizeMessage(msg), combinedLocal)
        );

        const merged = [...serverMessages];

        for (const local of combinedLocal) {
          const exists = merged.some(server =>
            server.id === local.id ||
            (!!server.clientTempId && !!local.clientTempId && server.clientTempId === local.clientTempId)
          );

          if (!exists) {
            merged.push(this.normalizeMessage(local));
          }
        }

        this.messages = merged.map(m => this.normalizeMessage(m));
        this.sortMessages();
        this.persistConversationMessages();
        this.cdr.detectChanges();
        this.scrollToBottom();
      },
      error: (err) => {
        console.error('Erreur chargement messages', err);

        // En cas d'erreur, charger les messages locaux (2 sources)
        const localMessagesById = this.loadLocalConversationMessages(conv.id);
        const localMessagesByParticipants = this.loadLocalConversationMessagesByParticipants(conv.otherParticipantId);
        
        const combined = [...localMessagesById];
        for (const msg of localMessagesByParticipants) {
          const exists = combined.some(m =>
            m.id === msg.id ||
            (!!m.clientTempId && !!msg.clientTempId && m.clientTempId === msg.clientTempId)
          );
          if (!exists) {
            combined.push(msg);
          }
        }

        this.messages = combined.map(m => this.normalizeMessage(m));
        this.sortMessages();
        this.cdr.detectChanges();
        this.scrollToBottom();
      }
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  async sendMessage(): Promise<void> {
    if ((!this.newMessage.trim() && this.selectedFiles.length === 0) || !this.selectedConv || this.isSending) {
      return;
    }

    this.isSending = true;
    this.showEmojiPicker = false;

    const currentUser = this.authService.getUser();
    const text = this.newMessage.trim();
    const nowIso = new Date().toISOString();
    const messageId = Date.now();
    const clientTempId = this.generateId();
    const filesToSend = [...this.selectedFiles];
    const replyToMessageId = this.replyingToMessage?.id ?? null;

    const localMsg: LocalMessage = {
      id: messageId,
      clientTempId,
      content: text,
      senderId: this.currentUserId,
      senderName: `${currentUser?.prenom ?? ''} ${currentUser?.nom ?? ''}`.trim(),
      receiverId: this.selectedConv.otherParticipantId,
      receiverName: this.selectedConv.otherParticipantName,
      sentAt: nowIso,
      read: false,
      replyToMessageId,
      reactions: [],
      attachments: filesToSend,
      pending: true,
      error: false
    };

    this.messages = [...this.messages, localMsg];
    this.sortMessages();
    this.persistConversationMessages();
    this.appendMessageToConversationStorage(this.selectedConv.otherParticipantId, localMsg);

    this.upsertConversationFromMessage(localMsg, false, {
      forcePreview: this.getConversationPreview(localMsg)
    });

    this.scrollToBottom();

    try {
      if (text) {
        this.wsService.sendMessage(
          this.selectedConv.otherParticipantId,
          text,
          clientTempId,
          messageId,
          replyToMessageId
        );
      }

      if (filesToSend.length > 0) {
        for (const file of filesToSend) {
          const base64Data = file.fileDataUrl.split(',')[1] || file.fileDataUrl;

          this.wsService.sendFile(
            this.selectedConv.otherParticipantId,
            messageId,
            {
              fileName: file.fileName,
              fileType: file.fileType,
              fileSize: file.fileSize,
              fileData: base64Data
            },
            clientTempId,
            replyToMessageId
          );
        }
      }
    } catch (error) {
      console.error('Erreur lors de l\'envoi:', error);

      const index = this.messages.findIndex(
        m => m.id === messageId || m.clientTempId === clientTempId
      );

      if (index !== -1) {
        this.messages[index] = {
          ...this.messages[index],
          pending: false,
          error: true
        };
        this.persistConversationMessages();
      }
    } finally {
      this.newMessage = '';
      this.selectedFiles = [];
      this.replyingToMessage = null;
      this.isSending = false;

      const ta = this.messageTextareaRef?.nativeElement;
      if (ta) {
        ta.style.height = 'auto';
      }

      this.cdr.detectChanges();
    }
  }

  downloadFile(file: MessageAttachment): void {
    const link = document.createElement('a');
    link.href = file.fileDataUrl;
    link.download = file.fileName;
    link.click();
  }

  private handleIncomingMessage(incomingMessage: LocalMessage): void {
    const otherParticipantId =
      incomingMessage.senderId === this.currentUserId
        ? incomingMessage.receiverId
        : incomingMessage.senderId;

    const isConversationOpen =
      !!this.selectedConv &&
      this.selectedConv.otherParticipantId === otherParticipantId;

    const pendingIndex = this.findMatchingPendingMessageIndex(incomingMessage);

    if (pendingIndex !== -1) {
      const localPending = this.messages[pendingIndex];

      const updatedMessage: LocalMessage = this.normalizeMessage({
        ...localPending,
        ...incomingMessage,
        id: incomingMessage.id || localPending.id,
        clientTempId: incomingMessage.clientTempId || localPending.clientTempId,
        replyToMessageId: localPending.replyToMessageId ?? incomingMessage.replyToMessageId ?? null,
        reactions: localPending.reactions ?? incomingMessage.reactions ?? [],
        attachments: localPending.attachments ?? incomingMessage.attachments ?? [],
        pending: false,
        error: false
      });

      this.messages = this.messages.map((m, i) => i === pendingIndex ? updatedMessage : m);
      this.sortMessages();
      this.persistConversationMessages();
      this.appendMessageToConversationStorage(otherParticipantId, updatedMessage);

      this.upsertConversationFromMessage(updatedMessage, false, {
        forcePreview: this.getConversationPreview(updatedMessage)
      });

      if (isConversationOpen) {
        this.scrollToBottom();
      }

      this.cdr.detectChanges();
      return;
    }

    if (this.findDuplicateMessageIndex(incomingMessage) !== -1) {
      return;
    }

    const hydrated = this.applyLocalMetaToSingleMessage(incomingMessage);

    if (isConversationOpen) {
      this.messages = [...this.messages, hydrated];
      this.sortMessages();
      this.persistConversationMessages();
      this.scrollToBottom();
    }

    this.appendMessageToConversationStorage(otherParticipantId, hydrated);

    this.upsertConversationFromMessage(
      hydrated,
      hydrated.senderId !== this.currentUserId,
      {
        forcePreview: this.getConversationPreview(hydrated)
      }
    );

    this.cdr.detectChanges();
  }

  private handleReactionMessage(reactionMsg: WebSocketMessage): void {
    const targetMessageId = reactionMsg.messageId;
    if (!targetMessageId) return;

    const message = this.messages.find(m => m.id === targetMessageId);
    if (!message) {
      return;
    }

    if (!message.reactions) {
      message.reactions = [];
    }

    const emoji = reactionMsg.emoji || '';
    const senderId = reactionMsg.senderId;
    const senderName = reactionMsg.senderName;

    if (reactionMsg.isAdd) {
      const existing = message.reactions.find(r => r.userId === senderId && r.emoji === emoji);
      if (!existing) {
        message.reactions.push({
          emoji,
          userId: senderId,
          userName: senderName
        });
      }
    } else {
      const index = message.reactions.findIndex(r => r.userId === senderId && r.emoji === emoji);
      if (index !== -1) {
        message.reactions.splice(index, 1);
      }
    }

    this.persistConversationMessages();
    this.cdr.detectChanges();
  }

  private handleFileMessage(fileMsg: WebSocketMessage): void {
    if (!fileMsg.fileName || !fileMsg.fileType || !fileMsg.fileData || !fileMsg.messageId) {
      console.warn('Message fichier incomplet:', fileMsg);
      return;
    }

    const attachment: MessageAttachment = {
      id: this.generateId(),
      fileName: fileMsg.fileName,
      fileType: fileMsg.fileType,
      fileSize: fileMsg.fileSize ?? 0,
      fileDataUrl: `data:${fileMsg.fileType};base64,${fileMsg.fileData}`
    };

    const messageDate = fileMsg.sentAt || new Date().toISOString();

    const otherParticipantId =
      fileMsg.senderId === this.currentUserId ? fileMsg.receiverId : fileMsg.senderId;

    const targetIndex = this.messages.findIndex(m =>
      m.id === fileMsg.messageId ||
      (!!fileMsg.clientTempId && m.clientTempId === fileMsg.clientTempId)
    );

    let finalMessage: LocalMessage;

    if (targetIndex !== -1) {
      const existingMessage = { ...this.messages[targetIndex] };
      const attachments = [...(existingMessage.attachments ?? [])];

      const alreadyExists = attachments.some(
        a =>
          a.fileName === attachment.fileName &&
          a.fileSize === attachment.fileSize &&
          a.fileType === attachment.fileType
      );

      if (!alreadyExists) {
        attachments.push(attachment);
      }

      finalMessage = this.normalizeMessage({
        ...existingMessage,
        id: fileMsg.messageId,
        clientTempId: fileMsg.clientTempId || existingMessage.clientTempId,
        sentAt: messageDate,
        replyToMessageId: existingMessage.replyToMessageId ?? fileMsg.replyToMessageId ?? null,
        attachments,
        pending: false,
        error: false
      });

      this.messages[targetIndex] = finalMessage;
    } else {
      finalMessage = {
        id: fileMsg.messageId,
        clientTempId: fileMsg.clientTempId,
        content: '',
        senderId: fileMsg.senderId,
        senderName: fileMsg.senderName,
        receiverId: fileMsg.receiverId,
        receiverName: fileMsg.receiverName || this.findUserNameById(fileMsg.receiverId),
        sentAt: messageDate,
        read: false,
        replyToMessageId: fileMsg.replyToMessageId ?? null,
        reactions: [],
        attachments: [attachment],
        pending: false,
        error: false
      };

      const isConversationOpen =
        !!this.selectedConv &&
        this.selectedConv.otherParticipantId === otherParticipantId;

      if (isConversationOpen) {
        this.messages = [...this.messages, finalMessage];
      }
    }

    this.sortMessages();
    if (this.selectedConv && this.selectedConv.otherParticipantId === otherParticipantId) {
      this.persistConversationMessages();
    }
    this.appendMessageToConversationStorage(otherParticipantId, finalMessage);

    this.upsertConversationFromMessage(
      finalMessage,
      fileMsg.senderId !== this.currentUserId,
      {
        forcePreview: `📎 ${fileMsg.fileName}`
      }
    );

    if (this.selectedConv && this.selectedConv.otherParticipantId === otherParticipantId) {
      this.scrollToBottom();
    }

    this.cdr.detectChanges();
  }

  private upsertConversationFromMessage(
    message: LocalMessage,
    incrementUnread: boolean,
    options?: { forcePreview?: string }
  ): void {
    const otherParticipantId =
      message.senderId === this.currentUserId ? message.receiverId : message.senderId;

    const otherParticipantName =
      message.senderId === this.currentUserId
        ? (message.receiverName || this.findUserNameById(message.receiverId))
        : (message.senderName || this.findUserNameById(message.senderId));

    const isCurrentlyOpen =
      !!this.selectedConv &&
      this.selectedConv.otherParticipantId === otherParticipantId;

    const preview = options?.forcePreview || this.getConversationPreview(message);

    const index = this.conversations.findIndex(c => c.otherParticipantId === otherParticipantId);

    if (index === -1) {
      this.conversations = [{
        id: -Math.floor(Date.now() + Math.random() * 1000),
        otherParticipantId,
        otherParticipantName: otherParticipantName || 'Utilisateur',
        lastMessage: preview,
        lastMessageTime: message.sentAt,
        unreadCount: incrementUnread && !isCurrentlyOpen ? 1 : 0
      }, ...this.conversations];

      this.sortConversations();
      this.updateSelectedConversationPreview(otherParticipantId, preview, message.sentAt);
      this.cdr.detectChanges();
      return;
    }

    const conv = this.conversations[index];
    const unreadCount = isCurrentlyOpen ? 0 : incrementUnread ? (conv.unreadCount || 0) + 1 : conv.unreadCount || 0;

    this.conversations = this.conversations.map((c, i) => i === index ? {
      ...conv,
      otherParticipantName: otherParticipantName || conv.otherParticipantName,
      lastMessage: preview,
      lastMessageTime: message.sentAt,
      unreadCount
    } : c);

    this.sortConversations();
    this.updateSelectedConversationPreview(otherParticipantId, preview, message.sentAt);
    this.cdr.detectChanges();
  }

  private updateSelectedConversationPreview(
    otherParticipantId: number,
    preview: string,
    sentAt: string
  ): void {
    if (this.selectedConv && this.selectedConv.otherParticipantId === otherParticipantId) {
      this.selectedConv = {
        ...this.selectedConv,
        lastMessage: preview,
        lastMessageTime: sentAt,
        unreadCount: 0
      };
    }
  }

  private getConversationPreview(message: Partial<LocalMessage>): string {
    const attachments = message.attachments ?? [];
    const text = (message.content || '').trim();

    if (attachments.length > 0) {
      if (attachments.length === 1) {
        return `📎 ${attachments[0].fileName}`;
      }
      return `📎 ${attachments.length} fichiers`;
    }

    if (text) {
      return text;
    }

    return '[Pièce jointe]';
  }

  private findUserNameById(userId: number): string {
    const user = this.usersList.find(u => u.id === userId);
    return user ? `${user.prenom} ${user.nom}` : 'Utilisateur';
  }

  private findMatchingPendingMessageIndex(msg: LocalMessage): number {
    return this.messages.findIndex(m => {
      if (!m.pending) return false;

      if (m.clientTempId && msg.clientTempId && m.clientTempId === msg.clientTempId) {
        return true;
      }

      if (m.id && msg.id && m.id === msg.id) {
        return true;
      }

      return (
        m.senderId === msg.senderId &&
        m.receiverId === msg.receiverId &&
        (m.content || '') === (msg.content || '') &&
        Math.abs(new Date(m.sentAt).getTime() - new Date(msg.sentAt).getTime()) < 15000
      );
    });
  }

  private findDuplicateMessageIndex(msg: LocalMessage): number {
    return this.messages.findIndex(m => {
      if (m.pending) return false;

      if (m.clientTempId && msg.clientTempId && m.clientTempId === msg.clientTempId) {
        return true;
      }

      if (m.id === msg.id) {
        return true;
      }

      return (
        m.senderId === msg.senderId &&
        m.receiverId === msg.receiverId &&
        (m.content || '') === (msg.content || '') &&
        Math.abs(new Date(m.sentAt).getTime() - new Date(msg.sentAt).getTime()) < 5000
      );
    });
  }

  private sortMessages(): void {
    this.messages = [...this.messages].sort(
      (a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
    );
  }

  private sortConversations(): void {
    this.conversations = [...this.conversations].sort(
      (a, b) => new Date(b.lastMessageTime).getTime() - new Date(a.lastMessageTime).getTime()
    );
  }

  normalizeMessage(msg: Message | LocalMessage): LocalMessage {
    return {
      ...msg,
      replyToMessageId: msg.replyToMessageId ?? null,
      reactions: msg.reactions ?? [],
      attachments: msg.attachments ?? [],
      pending: (msg as LocalMessage).pending ?? false,
      clientTempId: (msg as LocalMessage).clientTempId,
      error: (msg as LocalMessage).error ?? false
    };
  }

  private getConversationStorageKey(id: number): string {
    return `messages_conversation_${id}`;
  }

  private getConversationStorageKeyByParticipants(otherParticipantId: number): string {
    const sortedIds = [this.currentUserId, otherParticipantId].sort((a, b) => a - b);
    return `messages_conversation_users_${sortedIds[0]}_${sortedIds[1]}`;
  }

  private persistConversationMessages(): void {
    if (!this.selectedConv || this.selectedConv.id <= 0) return;

    try {
      localStorage.setItem(
        this.getConversationStorageKey(this.selectedConv.id),
        JSON.stringify(this.messages)
      );
      localStorage.setItem(
        this.getConversationStorageKeyByParticipants(this.selectedConv.otherParticipantId),
        JSON.stringify(this.messages)
      );
    } catch (e) {
      console.warn('Erreur localStorage:', e);
    }
  }

  private persistMessagesForConversation(conversationId: number, messages: LocalMessage[], otherParticipantId?: number): void {
    if (!conversationId || conversationId <= 0) return;

    try {
      localStorage.setItem(
        this.getConversationStorageKey(conversationId),
        JSON.stringify(messages.map(m => this.normalizeMessage(m)))
      );
      if (otherParticipantId) {
        localStorage.setItem(
          this.getConversationStorageKeyByParticipants(otherParticipantId),
          JSON.stringify(messages.map(m => this.normalizeMessage(m)))
        );
      }
    } catch (error) {
      console.warn('Erreur persistConversationMessages:', error);
    }
  }

  private resolveConversationIdByParticipant(otherParticipantId: number): number | null {
    const conv = this.conversations.find(c => c.otherParticipantId === otherParticipantId);
    if (conv && conv.id > 0) {
      return conv.id;
    }

    if (
      this.selectedConv &&
      this.selectedConv.otherParticipantId === otherParticipantId &&
      this.selectedConv.id > 0
    ) {
      return this.selectedConv.id;
    }

    return null;
  }

  private appendMessageToConversationStorage(otherParticipantId: number, message: LocalMessage): void {
    const conversationId = this.resolveConversationIdByParticipant(otherParticipantId);
    
    const localMessages = conversationId 
      ? this.loadLocalConversationMessages(conversationId)
      : this.loadLocalConversationMessagesByParticipants(otherParticipantId);

    const exists = localMessages.some(m =>
      m.id === message.id ||
      (!!m.clientTempId && !!message.clientTempId && m.clientTempId === message.clientTempId)
    );

    if (exists) {
      const updated = localMessages.map(m => {
        if (
          m.id === message.id ||
          (!!m.clientTempId && !!message.clientTempId && m.clientTempId === message.clientTempId)
        ) {
          return this.normalizeMessage({
            ...m,
            ...message,
            attachments: (message.attachments && message.attachments.length > 0)
              ? message.attachments
              : (m.attachments ?? []),
            pending: false,
            error: false
          });
        }
        return this.normalizeMessage(m);
      });

      if (conversationId) {
        this.persistMessagesForConversation(conversationId, updated, otherParticipantId);
      } else {
        this.persistMessagesForConversationByParticipants(otherParticipantId, updated);
      }
      return;
    }

    const updated = [...localMessages, this.normalizeMessage(message)];
    updated.sort((a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime());

    if (conversationId) {
      this.persistMessagesForConversation(conversationId, updated, otherParticipantId);
    } else {
      this.persistMessagesForConversationByParticipants(otherParticipantId, updated);
    }
  }

  private loadLocalConversationMessages(id: number): LocalMessage[] {
    try {
      const raw = localStorage.getItem(this.getConversationStorageKey(id));
      if (!raw) return [];
      return (JSON.parse(raw) as LocalMessage[]).map(m => this.normalizeMessage(m));
    } catch {
      return [];
    }
  }

  private loadLocalConversationMessagesByParticipants(otherParticipantId: number): LocalMessage[] {
    try {
      const raw = localStorage.getItem(this.getConversationStorageKeyByParticipants(otherParticipantId));
      if (!raw) return [];
      return (JSON.parse(raw) as LocalMessage[]).map(m => this.normalizeMessage(m));
    } catch {
      return [];
    }
  }

  private persistMessagesForConversationByParticipants(otherParticipantId: number, messages: LocalMessage[]): void {
    try {
      localStorage.setItem(
        this.getConversationStorageKeyByParticipants(otherParticipantId),
        JSON.stringify(messages.map(m => this.normalizeMessage(m)))
      );
    } catch (error) {
      console.warn('Erreur persistMessagesForConversationByParticipants:', error);
    }
  }

  private applyLocalMetaToSingleMessage(msg: LocalMessage, localMessages?: LocalMessage[]): LocalMessage {
    const localSource =
      localMessages ??
      (this.selectedConv && this.selectedConv.id > 0
        ? this.loadLocalConversationMessages(this.selectedConv.id)
        : []);

    const local = localSource.find(m =>
      m.id === msg.id ||
      (!!m.clientTempId && !!msg.clientTempId && m.clientTempId === msg.clientTempId)
    );

    if (!local) return this.normalizeMessage(msg);

    return this.normalizeMessage({
      ...msg,
      replyToMessageId: local.replyToMessageId ?? msg.replyToMessageId ?? null,
      reactions: local.reactions ?? msg.reactions ?? [],
      attachments: (local.attachments && local.attachments.length > 0)
        ? local.attachments
        : (msg.attachments ?? []),
      pending: false,
      clientTempId: local.clientTempId ?? msg.clientTempId,
      error: false
    });
  }

  startReply(message: LocalMessage): void {
    this.replyingToMessage = message;
    setTimeout(() => this.messageTextareaRef?.nativeElement.focus(), 0);
  }

  cancelReply(): void {
    this.replyingToMessage = null;
  }

  findMessageById(id: number | null | undefined): LocalMessage | null {
    if (!id) return null;
    return this.messages.find(m => m.id === id) ?? null;
  }

  addReaction(message: LocalMessage, emoji: string): void {
    const currentUser = this.authService.getUser();
    if (!currentUser || !currentUser.id) {
      return;
    }

    if (!message.reactions) {
      message.reactions = [];
    }

    const userId = currentUser.id;
    const existingIndex = message.reactions.findIndex(r => r.userId === userId && r.emoji === emoji);
    const isAdd = existingIndex === -1;

    if (existingIndex !== -1) {
      message.reactions.splice(existingIndex, 1);
    } else {
      message.reactions.push({
        emoji,
        userId,
        userName: `${currentUser.prenom} ${currentUser.nom}`.trim()
      });
    }

    this.persistConversationMessages();
    this.cdr.detectChanges();

    if (this.selectedConv && this.selectedConv.otherParticipantId) {
      this.wsService.sendReaction(
        this.selectedConv.otherParticipantId,
        message.id,
        emoji,
        isAdd
      );
    }
  }

  getReactionGroups(message: LocalMessage): { emoji: string; count: number }[] {
    if (!message.reactions || message.reactions.length === 0) {
      return [];
    }

    const grouped = new Map<string, number>();
    message.reactions.forEach(reaction => {
      if (reaction?.emoji) {
        grouped.set(reaction.emoji, (grouped.get(reaction.emoji) || 0) + 1);
      }
    });

    return Array.from(grouped.entries()).map(([emoji, count]) => ({ emoji, count }));
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    Array.from(input.files).forEach(file => {
      if (file.size > this.maxFileSizeBytes) {
        alert(`"${file.name}" dépasse 1 MB.`);
        return;
      }

      const reader = new FileReader();
      reader.onload = (e) => {
        const result = e.target?.result as string;

        const newAttachment: MessageAttachment = {
          id: this.generateId(),
          fileName: file.name,
          fileType: file.type,
          fileSize: file.size,
          fileDataUrl: result
        };

        const alreadyExists = this.selectedFiles.some(
          f =>
            f.fileName === newAttachment.fileName &&
            f.fileSize === newAttachment.fileSize &&
            f.fileType === newAttachment.fileType
        );

        if (!alreadyExists) {
          this.selectedFiles = [...this.selectedFiles, newAttachment];
          this.cdr.detectChanges();
        }
      };

      reader.onerror = (error) => {
        console.error('Erreur lecture fichier:', error);
      };

      reader.readAsDataURL(file);
    });

    input.value = '';
  }

  removeSelectedFile(index: number): void {
    this.selectedFiles.splice(index, 1);
    this.selectedFiles = [...this.selectedFiles];
    this.cdr.detectChanges();
  }

  formatFileSize(size: number): string {
    if (size < 1024) return `${size} o`;
    if (size < 1024 * 1024) return `${Math.round(size / 1024)} Ko`;
    return `${(size / (1024 * 1024)).toFixed(1)} Mo`;
  }

  isImage(fileType: string): boolean {
    return fileType?.startsWith('image/') ?? false;
  }

  getFileIcon(fileType: string): string {
    if (!fileType) return '📄';
    if (fileType.startsWith('image/')) return '🖼️';
    if (fileType.includes('pdf')) return '📕';
    if (fileType.includes('word') || fileType.includes('document')) return '📝';
    if (fileType.includes('sheet') || fileType.includes('excel')) return '📊';
    if (fileType.includes('zip') || fileType.includes('archive')) return '🗜️';
    if (fileType.startsWith('video/')) return '🎬';
    if (fileType.startsWith('audio/')) return '🎵';
    return '📎';
  }

  openNewMessageModal(): void {
    this.showNewMessageModal = true;
    this.newMessageReceiverId = null;
  }

  closeNewMessageModal(): void {
    this.showNewMessageModal = false;
    this.newMessageReceiverId = null;
  }

  createNewConversation(): void {
    if (!this.newMessageReceiverId) return;

    this.messageService.getOrCreateConversation(this.newMessageReceiverId).subscribe({
      next: (conv: Conversation) => {
        const exists = this.conversations.some(c => c.id === conv.id);
        this.closeNewMessageModal();

        if (!exists) {
          this.conversations = [{ ...conv, unreadCount: 0 }, ...this.conversations];
        }

        this.sortConversations();
        this.selectConversation(conv);
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur création conversation', err)
    });
  }

  openImageFullscreen(url: string): void {
    this.lightboxUrl = url;
  }

  closeLightbox(): void {
    this.lightboxUrl = null;
  }

  closeChat(): void {
    if (this.selectedConv) {
      this.conversations = this.conversations.map(c =>
        c.otherParticipantId === this.selectedConv!.otherParticipantId
          ? { ...c, unreadCount: 0 } : c
      );
    }

    this.selectedConv = null;
    this.messages = [];
    this.replyingToMessage = null;
    this.selectedFiles = [];
    this.showEmojiPicker = false;
    this.cdr.detectChanges();
  }

  getAvatarColor(name: string): string {
    const colors = ['#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#3b82f6', '#ec4899', '#14b8a6', '#f97316'];
    let hash = 0;

    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }

    return colors[Math.abs(hash) % colors.length];
  }

  scrollToBottom(): void {
    setTimeout(() => {
      try {
        const el = this.chatMessagesContainer.nativeElement;
        el.scrollTop = el.scrollHeight;
      } catch {}
    }, 50);
  }

  autoResize(event: Event): void {
    const ta = event.target as HTMLTextAreaElement;
    this.autoResizeTextarea(ta);
  }

  private autoResizeTextarea(ta: HTMLTextAreaElement): void {
    ta.style.height = 'auto';
    ta.style.height = Math.min(ta.scrollHeight, 120) + 'px';
  }

  private generateId(): string {
    return `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
  }
}