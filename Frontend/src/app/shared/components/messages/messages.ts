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
  MessageAttachment,
  MessageReaction
} from '../../../core/services/message.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { WebSocketService, WebSocketMessage } from '../../../core/services/websocket.service';
import { Subscription, firstValueFrom } from 'rxjs';

type PendingUploadFile = {
  id: string;
  file: File;
  fileName: string;
  fileType: string;
  fileSize: number;
};

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
  selectedFiles: PendingUploadFile[] = [];

  lightboxUrl: string | null = null;

  showEmojiPicker = false;
  emojiList = ['😊', '😂', '❤️', '👍', '🎉', '🔥', '😍', '🙏', '😢', '😎', '👏', '🤔', '😅', '💪', '✅', '🫡', '😆', '🥳', '👀', '💯'];

  readonly maxFileSizeBytes = 10 * 1024 * 1024;

  // Recherche utilisateur
  searchQuery = '';
  filteredUsers: User[] = [];
  isUserSelected = false;

  // ✅ Statut des utilisateurs en ligne
  userStatusMap = new Map<number, boolean>();
  

  
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
    this.currentUserId = Number(user?.id ?? 0);

    this.wsService.connect();

    this.wsSubscription = this.wsService.getMessages().subscribe((wsMsg: WebSocketMessage) => {
      if (!wsMsg) return;

      console.log('📩 WS component reçu:', wsMsg);

      // Gérer les messages de statut
      if (wsMsg.type === 'user_status') {
        this.handleUserStatusUpdate(wsMsg);
        return;
      }

      if (wsMsg.type === 'reaction') {
        this.handleReactionMessage(wsMsg);
        return;
      }

      const incomingMessage: LocalMessage = {
        id: wsMsg.id ?? wsMsg.messageId ?? Date.now(),
        clientTempId: wsMsg.clientTempId,
        content: wsMsg.content || '',
        senderId: Number(wsMsg.senderId),
        senderName: wsMsg.senderName || '',
        receiverId: Number(wsMsg.receiverId),
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

  // Helpers pour éviter les erreurs de type dans le template
  getUserFullName(user: User): string {
    return `${user.prenom ?? ''} ${user.nom ?? ''}`.trim();
  }

  getUserInitials(user: User): string {
    const p = (user.prenom ?? '').charAt(0).toUpperCase();
    const n = (user.nom ?? '').charAt(0).toUpperCase();
    return `${p}${n}`;
  }

  // ✅ Méthodes pour le statut en ligne
getUserStatusText(userId: number): string {
  return this.userStatusMap.get(userId) ? 'En ligne' : 'Déconnecté';
}

getUserStatusDotClass(userId: number): string {
  return this.userStatusMap.get(userId) ? 'msg-status-dot-online' : 'msg-status-dot-offline';
}

getUserStatusTextClass(userId: number): string {
  return this.userStatusMap.get(userId) ? 'status-online' : 'status-offline';
}
  filterUsers(): void {
    const q = this.searchQuery?.trim().toLowerCase() || '';
    
    if (!q) {
      this.filteredUsers = [];
      this.isUserSelected = false;
      this.cdr.detectChanges();
      return;
    }
    
    this.isUserSelected = false;
    this.filteredUsers = this.usersList.filter(u => {
      const fullName = `${u.prenom ?? ''} ${u.nom ?? ''}`.trim().toLowerCase();
      const email = (u.email ?? '').toLowerCase();
      return fullName.includes(q) || email.includes(q);
    });
    this.cdr.detectChanges();
  }

  selectUser(user: User): void {
    this.newMessageReceiverId = Number(user.id);
    this.searchQuery = this.getUserFullName(user);
    this.isUserSelected = true;
    this.filteredUsers = [];
    this.cdr.detectChanges();
  }

  clearSelectedUser(): void {
    this.searchQuery = '';
    this.newMessageReceiverId = null;
    this.isUserSelected = false;
    this.filteredUsers = [];
    this.cdr.detectChanges();
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
        if (!inList) { result.push('<ul class="msg-list">'); inList = true; }
        result.push(`<li>${line.substring(2)}</li>`);
      } else {
        if (inList) { result.push('</ul>'); inList = false; }
        result.push(line);
      }
    }

    if (inList) result.push('</ul>');
    html = result.join('\n').replace(/\n/g, '<br>');
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
      this.newMessage = this.newMessage.substring(0, start) + emoji + this.newMessage.substring(end);
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

    this.newMessage = this.newMessage.substring(0, start) + replacement + this.newMessage.substring(end);
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
        this.usersList = users.filter(u => Number(u.id) !== this.currentUserId);
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
            c.otherParticipantId === conv.otherParticipantId ? { ...realConv, unreadCount: 0 } : c
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
  this.showEmojiPicker = false;

  this.conversations = this.conversations.map(c =>
    c.id === conv.id ? { ...c, unreadCount: 0 } : c
  );

  this.messageService.getMessages(conv.id).subscribe({
    next: (msgs: Message[]) => {
      this.messages = msgs.map(msg => this.normalizeMessage(msg));
      this.sortMessages();
      this.cdr.detectChanges();
      this.scrollToBottom();
    },
    error: (err) => {
      console.error('Erreur chargement messages', err);
      this.messages = [];
      this.cdr.detectChanges();
    }
  });
  
  // ✅ Demander le statut initial de l'utilisateur
  this.wsService.checkUserStatus(conv.otherParticipantId);
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

    const localAttachments: MessageAttachment[] = filesToSend.map(file => ({
      id: file.id,
      fileName: file.fileName,
      fileType: file.fileType,
      fileSize: file.fileSize,
      fileUrl: ''
    }));

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
      attachments: localAttachments,
      pending: true,
      error: false
    };

    this.messages = [...this.messages, localMsg];
    this.sortMessages();
    this.scrollToBottom();
    this.cdr.detectChanges();

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

      for (const file of filesToSend) {
        await firstValueFrom(
          this.messageService.sendFile(
            this.selectedConv.otherParticipantId,
            file.file,
            clientTempId,
            replyToMessageId
          )
        );
      }

      if (this.selectedConv?.id) {
        const localReactionsMap = new Map<number, MessageReaction[]>();
        this.messages.forEach(m => {
          if (m.reactions && m.reactions.length > 0) {
            localReactionsMap.set(Number(m.id), [...m.reactions]);
          }
        });

        const refreshed = await firstValueFrom(this.messageService.getMessages(this.selectedConv.id));

        this.messages = refreshed.map(msg => {
          const normalized = this.normalizeMessage(msg);
          const serverReactions = normalized.reactions ?? [];
          const localReactions = localReactionsMap.get(Number(normalized.id)) ?? [];
          normalized.reactions = serverReactions.length > 0 ? serverReactions : localReactions;
          return normalized;
        });

        this.sortMessages();
      }

      this.loadConversations();
    } catch (error) {
      console.error('Erreur lors de l\'envoi:', error);
      const index = this.messages.findIndex(
        m => m.id === messageId || m.clientTempId === clientTempId
      );
      if (index !== -1) {
        this.messages[index] = { ...this.messages[index], pending: false, error: true };
        this.messages = [...this.messages];
      }
    } finally {
      this.newMessage = '';
      this.selectedFiles = [];
      this.replyingToMessage = null;
      this.isSending = false;
      const ta = this.messageTextareaRef?.nativeElement;
      if (ta) ta.style.height = 'auto';
      this.cdr.detectChanges();
      this.scrollToBottom();
    }
  }

  downloadFile(file: MessageAttachment): void {
    if (!file.fileUrl) return;
    const relativePath = file.fileUrl.replace('http://localhost:8080', '');
    const downloadUrl = `http://localhost:8080/api/files/download?path=${encodeURIComponent(relativePath)}`;
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = file.fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  private handleIncomingMessage(incomingMessage: LocalMessage): void {
    const otherParticipantId =
      incomingMessage.senderId === this.currentUserId
        ? incomingMessage.receiverId
        : incomingMessage.senderId;

    const isConversationOpen =
      !!this.selectedConv && this.selectedConv.otherParticipantId === otherParticipantId;

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
      if (isConversationOpen) this.scrollToBottom();
      this.cdr.detectChanges();
      return;
    }

    if (this.findDuplicateMessageIndex(incomingMessage) !== -1) return;

    const hydrated = this.normalizeMessage(incomingMessage);
    if (isConversationOpen) {
      this.messages = [...this.messages, hydrated];
      this.sortMessages();
      this.scrollToBottom();
    }

    this.loadConversations();
    this.cdr.detectChanges();
  }

  private handleReactionMessage(reactionMsg: WebSocketMessage): void {
    console.log('=== handleReactionMessage ===', reactionMsg);
    if (Number(reactionMsg.senderId) === this.currentUserId) {
      return;
    }
    this.applyReactionLocally(reactionMsg);
    this.loadConversations();
  }

  private applyReactionLocally(wsMsg: WebSocketMessage): void {
    if (!wsMsg.messageId || !wsMsg.emoji || !wsMsg.senderId) return;

    const index = this.messages.findIndex(m => Number(m.id) === Number(wsMsg.messageId));
    if (index === -1) return;

    const currentReactions = [...(this.messages[index].reactions ?? [])];

    const exists = currentReactions.some(
      r => Number(r.userId) === Number(wsMsg.senderId) && r.emoji === wsMsg.emoji
    );

    let updatedReactions: MessageReaction[];

    if (wsMsg.isAdd) {
      updatedReactions = exists
        ? currentReactions
        : [...currentReactions, {
            emoji: wsMsg.emoji,
            userId: Number(wsMsg.senderId),
            userName: wsMsg.senderName || ''
          }];
    } else {
      updatedReactions = currentReactions.filter(
        r => !(Number(r.userId) === Number(wsMsg.senderId) && r.emoji === wsMsg.emoji)
      );
    }

    this.messages = this.messages.map((m, i) =>
      i === index ? { ...m, reactions: updatedReactions } : m
    );
    this.cdr.detectChanges();
  }

  private findMatchingPendingMessageIndex(msg: LocalMessage): number {
    return this.messages.findIndex(m => {
      if (!m.pending) return false;
      if (m.clientTempId && msg.clientTempId && m.clientTempId === msg.clientTempId) return true;
      if (m.id && msg.id && m.id === msg.id) return true;
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
      if (m.clientTempId && msg.clientTempId && m.clientTempId === msg.clientTempId) return true;
      if (m.id === msg.id) return true;
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
    const attachments: MessageAttachment[] = [];
    const existingAttachments = msg.attachments ?? [];
    if (existingAttachments.length > 0) attachments.push(...existingAttachments);

    if ((msg.fileUrl ?? '').trim()) {
      const alreadyAdded = attachments.some(a => a.fileUrl === msg.fileUrl);
      if (!alreadyAdded) {
        attachments.push({
          fileName: msg.fileName || 'Fichier',
          fileType: msg.fileType || 'application/octet-stream',
          fileSize: msg.fileSize || 0,
          fileUrl: msg.fileUrl || ''
        });
      }
    }

    return {
      ...msg,
      id: Number(msg.id),
      senderId: Number(msg.senderId),
      receiverId: Number(msg.receiverId),
      replyToMessageId: msg.replyToMessageId ?? null,
      reactions: (msg.reactions ?? []).map((r: MessageReaction) => ({
        emoji: r.emoji,
        userId: Number(r.userId),
        userName: r.userName || ''
      })),
      attachments,
      pending: (msg as LocalMessage).pending ?? false,
      clientTempId: (msg as LocalMessage).clientTempId ?? msg.clientTempId,
      error: (msg as LocalMessage).error ?? false
    };
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
    return this.messages.find(m => Number(m.id) === Number(id)) ?? null;
  }

  addReaction(message: LocalMessage, emoji: string): void {
    this.toggleReaction(message, emoji);
  }

  toggleReaction(message: LocalMessage, emoji: string): void {
    const currentUser = this.authService.getUser();
    if (!currentUser || !currentUser.id) return;

    if (!message.id || message.id <= 0) {
      console.warn('Message sans id base de données, réaction impossible');
      return;
    }

    if (!this.selectedConv || !this.selectedConv.otherParticipantId) {
      console.warn('Conversation sélectionnée introuvable');
      return;
    }

    const index = this.messages.findIndex(m => Number(m.id) === Number(message.id));
    if (index === -1) {
      console.warn('Message introuvable dans la liste');
      return;
    }

    const existingReactions = [...(this.messages[index].reactions ?? [])];

    const alreadyExists = existingReactions.some(
      r => Number(r.userId) === Number(currentUser.id) && r.emoji === emoji
    );

    const updatedReactions = alreadyExists
      ? existingReactions.filter(
          r => !(Number(r.userId) === Number(currentUser.id) && r.emoji === emoji)
        )
      : [...existingReactions, {
          emoji,
          userId: Number(currentUser.id),
          userName: `${currentUser.prenom} ${currentUser.nom}`.trim()
        }];

    this.messages = this.messages.map((m, i) =>
      i === index ? { ...m, reactions: updatedReactions } : m
    );
    this.cdr.detectChanges();

    this.wsService.sendReaction(
      this.selectedConv.otherParticipantId,
      Number(message.id),
      emoji,
      !alreadyExists
    );
  }

  getReactionGroups(message: LocalMessage): { emoji: string; count: number }[] {
    if (!message.reactions || message.reactions.length === 0) return [];

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
        alert(`"${file.name}" dépasse 10 MB.`);
        return;
      }
      const alreadyExists = this.selectedFiles.some(
        f => f.fileName === file.name && f.fileSize === file.size && f.fileType === file.type
      );
      if (!alreadyExists) {
        this.selectedFiles = [...this.selectedFiles, {
          id: this.generateId(),
          file,
          fileName: file.name,
          fileType: file.type,
          fileSize: file.size
        }];
      }
    });

    input.value = '';
    this.cdr.detectChanges();
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
    this.searchQuery = '';
    this.filteredUsers = [];
    this.newMessageReceiverId = null;
    this.isUserSelected = false;
    this.cdr.detectChanges();
  }

  closeNewMessageModal(): void {
    this.showNewMessageModal = false;
    this.newMessageReceiverId = null;
    this.searchQuery = '';
    this.filteredUsers = [];
    this.isUserSelected = false;
    this.cdr.detectChanges();
  }

  createNewConversation(): void {
    if (!this.newMessageReceiverId) return;

    this.messageService.getOrCreateConversation(this.newMessageReceiverId).subscribe({
      next: (conv: Conversation) => {
        const exists = this.conversations.some(c => c.id === conv.id);
        this.closeNewMessageModal();
        if (!exists) this.conversations = [{ ...conv, unreadCount: 0 }, ...this.conversations];
        this.sortConversations();
        this.selectConversation(conv);
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur création conversation', err)
    });
  }

  openImageFullscreen(url: string): void { this.lightboxUrl = url; }
  closeLightbox(): void { this.lightboxUrl = null; }

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
    this.autoResizeTextarea(event.target as HTMLTextAreaElement);
  }

  private autoResizeTextarea(ta: HTMLTextAreaElement): void {
    ta.style.height = 'auto';
    ta.style.height = Math.min(ta.scrollHeight, 120) + 'px';
  }

  private generateId(): string {
    return `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
  }

  private reloadCurrentConversationMessages(): void {
    if (!this.selectedConv?.id) return;
    this.messageService.getMessages(this.selectedConv.id).subscribe({
      next: (msgs: Message[]) => {
        this.messages = msgs.map(msg => this.normalizeMessage(msg));
        this.sortMessages();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur reload messages après réaction', err)
    });
  }
   private handleUserStatusUpdate(wsMsg: WebSocketMessage): void {
    console.log('📡 Mise à jour statut reçue:', { userId: wsMsg.userId, online: wsMsg.online });
    if (wsMsg.type === 'user_status' && wsMsg.userId !== undefined) {
      this.userStatusMap.set(wsMsg.userId, wsMsg.online === true);
      console.log('📊 userStatusMap mis à jour:', Array.from(this.userStatusMap.entries()));
      this.cdr.detectChanges();
    }
  }

  private loadInitialUserStatus(): void {
  // Demander le statut de l'utilisateur de la conversation sélectionnée
  if (this.selectedConv?.otherParticipantId) {
    this.wsService.checkUserStatus(this.selectedConv.otherParticipantId);
  }
}
}