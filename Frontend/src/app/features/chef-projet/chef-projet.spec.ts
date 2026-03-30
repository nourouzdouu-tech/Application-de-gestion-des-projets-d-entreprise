import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChefProjet } from './chef-projet';

describe('ChefProjet', () => {
  let component: ChefProjet;
  let fixture: ComponentFixture<ChefProjet>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChefProjet]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChefProjet);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
