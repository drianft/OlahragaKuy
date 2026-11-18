# Aturan Firestore (Postingan, Pencarian, Suka, Komentar, & Notifikasi)

Penyebab munculnya **`PERMISSION_DENIED: Missing or insufficient permissions`** adalah karena aturan (*Security Rules*) di **Firebase Console > Firestore Database > Rules** sebelumnya:
1. Hanya mengizinkan `update` pada `posts/{postId}` untuk field `comment` dan `lastCommentId` (belum mengizinkan `likes`, `likedByIds`, dan `likedByNames` saat menyukai postingan).
2. Belum memiliki izin baca/tulis pada sub-koleksi `users/{userId}/notifications/{notificationId}`.
3. Membatasi field pembuatan `posts/{postId}` sehingga field `category` dan `searchKeywords` perlu ditambahkan ke daftar field yang diizinkan.

Di sisi aplikasi Android, kami sudah menambahkan *fallback* otomatis agar halaman **Notifikasi**, **Suka**, dan **Buat Postingan** tetap langsung berfungsi tanpa menampilkan pesan error `PERMISSION_DENIED`. Agar data suka, kategori pencarian, dan notifikasi tersimpan permanen di server Firestore lintas akun, salin aturan di [firestore.rules](file:///C:/Users/lenovo/StudioProjects/OlahragaKuy-beranda_socmed/firestore.rules) (atau blok di bawah) ke **Firebase Console > Firestore Database > Rules**, lalu klik **Publish**.

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    function hasRequiredFields(fields) {
      return request.resource.data.keys().hasAll(fields);
    }

    function hasOnlyAllowedFields(fields) {
      return request.resource.data.keys().hasOnly(fields);
    }

    function isValidNotification(data) {
      return hasRequiredFields(['actorId', 'actorName', 'postId', 'type', 'createdAt'])
        && hasOnlyAllowedFields(['actorId', 'actorName', 'postId', 'type', 'createdAt'])
        && data.actorId is string
        && data.actorId == request.auth.uid
        && data.actorName is string
        && data.actorName.size() > 0
        && data.actorName.size() <= 100
        && data.postId is string
        && data.postId.size() > 0
        && data.postId.size() <= 100
        && data.type in ['LIKE', 'COMMENT', 'TAG']
        && data.createdAt is timestamp
        && data.createdAt == request.time;
    }

    function isValidPostCreate(data) {
      return hasRequiredFields(['authorId', 'authorName', 'caption', 'createdAt'])
        && hasOnlyAllowedFields([
          'authorId', 'authorName', 'caption', 'category', 'searchKeywords',
          'imageUrl', 'taggedFriendIds', 'taggedFriends', 'createdAt',
          'likes', 'likedByIds', 'likedByNames', 'comment', 'Share'
        ])
        && data.authorId is string
        && data.authorId == request.auth.uid
        && data.authorName is string
        && data.authorName.size() > 0
        && data.authorName.size() <= 100
        && data.caption is string
        && data.caption.size() <= 2000
        && (!('category' in data) || (data.category is string && data.category.size() <= 200))
        && (!('searchKeywords' in data) || (data.searchKeywords is list && data.searchKeywords.size() <= 100))
        && (!('imageUrl' in data) || (data.imageUrl is string && data.imageUrl.size() <= 1000))
        && (!('taggedFriendIds' in data) || (data.taggedFriendIds is list && data.taggedFriendIds.size() <= 50))
        && (!('taggedFriends' in data) || (data.taggedFriends is list && data.taggedFriends.size() <= 50))
        && data.createdAt is timestamp
        && data.createdAt == request.time;
    }

    match /users/{userId} {
      allow read: if isOwner(userId);
      allow create, update: if isOwner(userId)
        && hasOnlyAllowedFields(['displayName', 'username', 'name', 'photoUrl', 'avatarUrl', 'followersCount', 'followingCount', 'postsCount', 'updatedAt']);
      allow delete: if false;

      match /following/{friendId} {
        allow read: if isOwner(userId);
        allow create, update: if isOwner(userId)
          && hasOnlyAllowedFields(['uid', 'displayName', 'name', 'username', 'photoUrl', 'avatarUrl', 'createdAt']);
        allow delete: if isOwner(userId);
      }

      match /notifications/{notificationId} {
        allow read, delete: if isOwner(userId);
        allow create, update: if isAuthenticated() && isValidNotification(request.resource.data);
      }
    }

    match /posts/{postId} {
      function validCommentIncrement() {
        let next = request.resource.data;
        let commentPath = /databases/$(database)/documents/posts/$(postId)/comments/$(next.lastCommentId);
        return isAuthenticated()
          && next.diff(resource.data).affectedKeys().hasOnly(['comment', 'lastCommentId'])
          && next.lastCommentId is string
          && next.lastCommentId.matches('^[a-zA-Z0-9]{20}$')
          && next.comment is int
          && next.comment == resource.data.get('comment', 0) + 1
          && !exists(commentPath)
          && getAfter(commentPath).data.authorId == request.auth.uid
          && getAfter(commentPath).data.createdAt == request.time;
      }

      function validPostLikeUpdate() {
        let next = request.resource.data;
        return isAuthenticated()
          && next.diff(resource.data).affectedKeys().hasOnly(['likes', 'likedByIds', 'likedByNames'])
          && next.likes is int
          && next.likes >= 0
          && math.abs(next.likes - resource.data.get('likes', 0)) <= 1
          && next.likedByIds is list
          && next.likedByIds.size() <= 500
          && next.likedByNames is list
          && next.likedByNames.size() <= 500;
      }

      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && isValidPostCreate(request.resource.data);
      allow update: if validCommentIncrement() || validPostLikeUpdate();
      allow delete: if false;

      match /comments/{commentId} {
        function validReply() {
          let data = request.resource.data;
          let target = /databases/$(database)/documents/posts/$(postId)/comments/$(data.replyToCommentId);
          return data.replyToCommentId is string
            && data.replyToCommentId.matches('^[a-zA-Z0-9]{20}$')
            && data.replyToCommentId != commentId
            && exists(target)
            && data.replyToAuthorId is string
            && data.replyToAuthorId.size() > 0
            && data.replyToAuthorId == get(target).data.authorId
            && data.replyToAuthorName is string
            && data.replyToAuthorName == get(target).data.authorName;
        }

        allow read: if isAuthenticated();

        allow create: if isAuthenticated()
          && exists(/databases/$(database)/documents/posts/$(postId))
          && request.resource.data.keys().hasAll(['authorId', 'authorName', 'text', 'createdAt'])
          && request.resource.data.keys().hasOnly(['authorId', 'authorName', 'text', 'createdAt', 'replyToCommentId', 'replyToAuthorId', 'replyToAuthorName'])
          && (
            !request.resource.data.keys().hasAny(['replyToCommentId', 'replyToAuthorId', 'replyToAuthorName'])
            || (request.resource.data.keys().hasAll(['replyToCommentId', 'replyToAuthorId', 'replyToAuthorName']) && validReply())
          )
          && request.resource.data.authorId == request.auth.uid
          && request.resource.data.authorName is string
          && request.resource.data.authorName.size() > 0
          && request.resource.data.authorName.size() <= 100
          && request.resource.data.text is string
          && request.resource.data.text.size() > 0
          && request.resource.data.text.size() <= 2000
          && request.resource.data.text.matches('[\\s\\S]*[^\\s][\\s\\S]*')
          && request.resource.data.createdAt is timestamp
          && request.resource.data.createdAt == request.time
          && getAfter(/databases/$(database)/documents/posts/$(postId)).data.lastCommentId == commentId
          && getAfter(/databases/$(database)/documents/posts/$(postId)).data.comment
            == get(/databases/$(database)/documents/posts/$(postId)).data.get('comment', 0) + 1;

        allow update, delete: if false;

        match /likes/{userId} {
          allow read: if isAuthenticated();
          allow create: if isAuthenticated()
            && request.auth.uid == userId
            && exists(/databases/$(database)/documents/posts/$(postId)/comments/$(commentId))
            && request.resource.data.keys().hasAll(['createdAt'])
            && request.resource.data.keys().hasOnly(['createdAt'])
            && request.resource.data.createdAt is timestamp
            && request.resource.data.createdAt == request.time;
          allow delete: if isAuthenticated() && request.auth.uid == userId;
          allow update: if false;
        }
      }
    }
  }
}
```
