import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResponsableContract } from './responsable-contract';

describe('ResponsableContract', () => {
  let component: ResponsableContract;
  let fixture: ComponentFixture<ResponsableContract>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResponsableContract]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ResponsableContract);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
