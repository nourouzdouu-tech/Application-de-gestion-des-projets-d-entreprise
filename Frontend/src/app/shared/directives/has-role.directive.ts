import {
  Directive,
  Input,
  TemplateRef,
  ViewContainerRef,
  inject
} from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

@Directive({
  selector: '[hasRole]',
  standalone: true
})
export class HasRoleDirective {
  private templateRef = inject(TemplateRef<any>);
  private viewContainer = inject(ViewContainerRef);
  private authService = inject(AuthService);

  @Input()
  set hasRole(value: string | string[]) {
    const roles = Array.isArray(value) ? value : [value];
    const allowed = roles.some(role => this.authService.hasRole(role));

    this.viewContainer.clear();

    if (allowed) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    }
  }
}