import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  host: {
    class: 'block min-h-screen',
  },
})
export class App {
  protected readonly title = signal('center');
}
