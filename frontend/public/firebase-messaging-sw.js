// Give the service worker access to Firebase Messaging.
// Note that you can only use Firebase Messaging here.
try {
  importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js');
  importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js');

  // Initialize the Firebase app in the service worker
  firebase.initializeApp({
    apiKey: "AIzaSyDQHWI_zhIjqp_SJz0Fdv7xtG6mIZfwBhU",
    authDomain: "growfarm-6cd79.firebaseapp.com",
    projectId: "growfarm-6cd79",
    storageBucket: "growfarm-6cd79.firebasestorage.app",
    messagingSenderId: "763805350293",
    appId: "1:763805350293:web:68b1b3ca3a294b749b1e9c",
    measurementId: "G-G9C4KYCEEJ"
  });

  // Retrieve an instance of Firebase Messaging so that it can handle background messages.
  const messaging = firebase.messaging();

  // Handle background messages
  messaging.onBackgroundMessage((payload) => {
    console.log('[firebase-messaging-sw.js] Received background message ', payload);
    
    // Customize notification here
    const notificationTitle = payload.notification.title || '알림';
    const notificationOptions = {
      body: payload.notification.body || '',
      icon: '/app_logo.png'
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
  });
  
  // 브라우저 푸시 API 이벤트 리스너 직접 추가
  // 백그라운드 상태일 때만 알림을 표시
  self.addEventListener('push', function(event) {
    // 클라이언트 목록을 확인하여 활성 창이 있는지 확인
    event.waitUntil(
      self.clients.matchAll({
        type: 'window',
        includeUncontrolled: true
      }).then(clients => {
        // 활성 창이 있는지 확인 (포그라운드 상태)
        const isClientFocused = clients.some(client => client.focused || client.visibilityState === 'visible');
        
        // 포그라운드 상태면 알림 표시하지 않음
        if (isClientFocused) {
          console.log('[firebase-messaging-sw.js] 포그라운드 상태이므로 알림 표시 안함');
          return;
        }
        
        // 백그라운드 상태일 때만 알림 표시
        if (event.data) {
          try {
            const data = event.data.json();
            const title = data.notification?.title || '알림';
            const options = {
              body: data.notification?.body || '',
              icon: '/app_logo.png',
            };
            
            console.log('[firebase-messaging-sw.js] 백그라운드 알림 표시:', data);
            return self.registration.showNotification(title, options);
          } catch (error) {
            console.error('[firebase-messaging-sw.js] Error processing push event:', error);
            // 데이터 파싱 실패 시 기본 알림 표시
            return self.registration.showNotification('새 알림', {
              body: '새로운 알림이 도착했습니다.',
              icon: '/app_logo.png'
            });
          }
        }
      })
    );
  });
} catch (error) {
  console.error('[firebase-messaging-sw.js] Error initializing service worker:', error);
} 