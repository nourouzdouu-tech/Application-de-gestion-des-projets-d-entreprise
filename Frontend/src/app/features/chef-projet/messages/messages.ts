import { Component, OnInit, OnDestroy, ElementRef, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessageService, Conversation, Message } from '../../../core/services/message.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService, User } from '../../../core/services/user.service';
import { WebSocketService, ChatMessage } from '../../../core/services/websocket.service';

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messages.html',
  styleUrls: ['./messages.css']
})
export class MessagesComponent implements OnInit, OnDestroy {
  conversations: Conversation[] = [];
  selectedConv: Conversation | null = null;
  messages: Message[] = [];
  newMessage = '';
  currentUserId: number = 0;
  showNewMessageModal = false;
  newMessageReceiverId: number | null = null;
  usersList: User[] = [];

  @ViewChild('chatMessages') private chatMessagesContainer!: ElementRef;

  constructor(
    private messageService: MessageService,
    private authService: AuthService,
    private userService: UserService,
    private wsService: WebSocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Récupérer l'id de l'utilisateur courant (doit être stocké dans le localStorage)
    const user = this.authService.getUser();
    this.currentUserId = user?.id ?? 0;
    if (!this.currentUserId) {
      console.error('Aucun id utilisateur trouvé, les messages risquent de mal s\'afficher');
    }

    // Connexion WebSocket
    this.wsService.connect();

    // Abonnement aux messages WebSocket
    this.wsService.getMessages().subscribe((wsMsg: ChatMessage) => {
      if (!wsMsg) return;
      console.log('Message WebSocket reçu:', wsMsg);
      const msg: Message = {
        id: wsMsg.id!,
        content: wsMsg.content,
        senderId: wsMsg.senderId,
        senderName: wsMsg.senderName,
        receiverId: wsMsg.receiverId,
        receiverName: wsMsg.receiverName || '',
        sentAt: wsMsg.sentAt || new Date().toISOString(),
        read: wsMsg.read || false
      };

      // 1. Si une conversation est ouverte et que ce message lui appartient
      if (this.selectedConv) {
        const otherId = this.selectedConv.otherParticipantId;
        const isForCurrentConv = (msg.senderId === otherId && msg.receiverId === this.currentUserId) ||
                                 (msg.senderId === this.currentUserId && msg.receiverId === otherId);
        if (isForCurrentConv) {
          // Éviter les doublons
          const exists = this.messages.some(m => m.id === msg.id);
          if (!exists) {
            this.messages.push(msg);
            this.scrollToBottom();
            // Mettre à jour le dernier message dans la conversation ouverte
            this.selectedConv.lastMessage = msg.content;
            this.selectedConv.lastMessageTime = msg.sentAt;
            if (msg.senderId !== this.currentUserId) {
              this.selectedConv.unreadCount = 0; // remettre à zéro car on lit
            }
          }
        }
      }

      // 2. Mettre à jour la conversation correspondante dans la liste latérale
      const otherParticipantId = msg.senderId === this.currentUserId ? msg.receiverId : msg.senderId;
      const conv = this.conversations.find(c => c.otherParticipantId === otherParticipantId);
      if (conv) {
        conv.lastMessage = msg.content;
        conv.lastMessageTime = msg.sentAt;
        // Incrémenter le compteur non lu seulement si le message est reçu et la conversation n'est pas active
        if (msg.senderId !== this.currentUserId && this.selectedConv?.otherParticipantId !== otherParticipantId) {
          conv.unreadCount++;
        } else if (msg.senderId !== this.currentUserId && this.selectedConv?.otherParticipantId === otherParticipantId) {
          conv.unreadCount = 0;
        }
      } else {
        // Nouvelle conversation, recharger la liste
        this.loadConversations();
      }

      this.cdr.detectChanges();
    });

    // Chargement initial
    this.loadConversations();
    this.loadUsersList();
  }

  ngOnDestroy(): void {
    this.wsService.disconnect();
  }

  loadConversations(): void {
    this.messageService.getConversations().subscribe({
      next: (data: Conversation[]) => {
        this.conversations = data;
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
      error: (err: any) => {
        console.error('Erreur chargement utilisateurs réels', err);
        // Fallback fictif (optionnel)
        this.usersList = [
          { id: 2, prenom: 'Safae', nom: 'Ibrahimi', email: 'safae@test.com', roles: [] },
          { id: 3, prenom: 'Samir', nom: 'Mostawi', email: 'samir@test.com', roles: [] },
          { id: 4, prenom: 'Ali', nom: 'Baha', email: 'ali@test.com', roles: [] }
        ].filter(u => u.id !== this.currentUserId);
        this.cdr.detectChanges();
      }
    });
  }

  selectConversation(conv: Conversation): void {
    this.selectedConv = conv;
    // Réinitialiser le compteur de non lus
    conv.unreadCount = 0;
    this.messageService.getMessages(conv.id).subscribe({
      next: (msgs: Message[]) => {
        this.messages = msgs;
        this.cdr.detectChanges();
        setTimeout(() => this.scrollToBottom(), 100);
      },
      error: (err) => console.error('Erreur chargement messages', err)
    });
  }

 sendMessage(event?: Event): void {
  if (event instanceof KeyboardEvent && event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
  }
  if (!this.newMessage.trim() || !this.selectedConv) return;

  // Ajout local immédiat (optimiste)
  const tempMsg: Message = {
    id: Date.now(),
    content: this.newMessage,
    senderId: this.currentUserId,
    senderName: `${this.authService.getUser()?.prenom} ${this.authService.getUser()?.nom}`,
    receiverId: this.selectedConv.otherParticipantId,
    receiverName: this.selectedConv.otherParticipantName,
    sentAt: new Date().toISOString(),
    read: false
  };
  this.messages.push(tempMsg);
  this.scrollToBottom();

  // Envoi réel via WebSocket
  this.wsService.sendMessage(this.selectedConv.otherParticipantId, this.newMessage);
  this.newMessage = '';

  // On ne supprime pas le message temporaire ; le vrai message arrivera plus tard
  // (on pourra le remplacer par son id plus tard si besoin)
}

  scrollToBottom(): void {
    try {
      this.chatMessagesContainer.nativeElement.scrollTop = this.chatMessagesContainer.nativeElement.scrollHeight;
    } catch (err) {}
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
        this.closeNewMessageModal();
        this.conversations.unshift(conv);
        this.selectConversation(conv);
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur création conversation', err)
    });
  }

  closeChat(): void {
    this.selectedConv = null;
    this.messages = [];
  }

  getAvatarColor(name: string): string {
    const colors = ['#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#3b82f6', '#ec4899', '#14b8a6', '#f97316'];
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }
}