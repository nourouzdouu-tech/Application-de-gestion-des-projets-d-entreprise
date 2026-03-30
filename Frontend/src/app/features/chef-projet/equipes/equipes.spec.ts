import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Equipes } from './equipes';

describe('Equipes', () => {
  let component: Equipes;
  let fixture: ComponentFixture<Equipes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Equipes]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Equipes);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
