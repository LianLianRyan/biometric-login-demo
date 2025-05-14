import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  private http = inject(HttpClient);
  username = signal('');
  registrationResult = signal<any>(null);

  async register() {
    if (!this.username()) {
      alert('Please enter a username.');
      return;
    }

    // Request backend to generate registration configuration
    const publicKeyOptions: any = await this.http
      .post('/api/auth/registerRequest', { username: this.username() })
      .toPromise();

    //  base64 challenge / id > Uint8Array
    publicKeyOptions.challenge = Uint8Array.from(atob(publicKeyOptions.challenge), c => c.charCodeAt(0));
    publicKeyOptions.user.id = Uint8Array.from(atob(publicKeyOptions.user.id), c => c.charCodeAt(0));

    try {
      const credential: any = await navigator.credentials.create({ publicKey: publicKeyOptions });

      console.log('credential:', credential);

      // Send to backend for registration confirmation
      const response = await this.http
        .post('/api/auth/registerResponse', {
          id: credential.id,
          rawId: btoa(String.fromCharCode(...new Uint8Array(credential.rawId))),
          response: {
            attestationObject: btoa(String.fromCharCode(...new Uint8Array(credential.response.attestationObject))),
            clientDataJSON: btoa(String.fromCharCode(...new Uint8Array(credential.response.clientDataJSON))),
          },
          type: credential.type,
          username: this.username(),
        })
        .toPromise();

      this.registrationResult.set(response);
    } catch (err) {
      console.error('Registration failed:', err);
    }
  }
}
