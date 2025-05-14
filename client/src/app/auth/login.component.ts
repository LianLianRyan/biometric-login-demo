import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private http = inject(HttpClient);
  username = signal('');
  loginResult = signal<any>(null);

  async login() {
    if (!this.username()) {
      alert('Please enter a username.');
      return;
    }

    //  challenge + allowCredentials
    const publicKeyOptions: any = await this.http
      .post('/api/auth/loginRequest', { username: this.username() })
      .toPromise();

    // Base64 transfer
    publicKeyOptions.challenge = Uint8Array.from(atob(publicKeyOptions.challenge), c => c.charCodeAt(0));
    if (publicKeyOptions.allowCredentials) {
      publicKeyOptions.allowCredentials = publicKeyOptions.allowCredentials.map((cred: any) => ({
        ...cred,
        id: Uint8Array.from(atob(cred.id), c => c.charCodeAt(0)),
      }));
    }

    try {
      const assertion: any = await navigator.credentials.get({ publicKey: publicKeyOptions });

      console.log('✅ Assertion result:', assertion);

      //  assertion 
      const response = await this.http
        .post('/api/auth/loginResponse', {
          id: assertion.id,
          rawId: btoa(String.fromCharCode(...new Uint8Array(assertion.rawId))),
          type: assertion.type,
          response: {
            authenticatorData: btoa(String.fromCharCode(...new Uint8Array(assertion.response.authenticatorData))),
            clientDataJSON: btoa(String.fromCharCode(...new Uint8Array(assertion.response.clientDataJSON))),
            signature: btoa(String.fromCharCode(...new Uint8Array(assertion.response.signature))),
            userHandle: assertion.response.userHandle
              ? btoa(String.fromCharCode(...new Uint8Array(assertion.response.userHandle)))
              : null,
          },
          username: this.username(),
        })
        .toPromise();

      this.loginResult.set(response);
    } catch (err) {
      console.error('Login failed:', err);
    }
  }
}

