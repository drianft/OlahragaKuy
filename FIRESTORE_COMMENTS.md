# Mengaktifkan komentar di Firestore

Untuk database `(default)` yang digunakan aplikasi. Di Firebase Console > Firestore Database > Rules, DI DALAM `match /posts/{postId}`, ganti bagian mulai `function validCommentIncrement()` sampai akhir blok `match /comments/{commentId}` lama dengan seluruh blok pertama di bawah. Jika masih memakai aturan awal, ganti baris `allow update, delete: if false;` pada post dengan blok ini. Pertahankan aturan read/create post serta kurung penutup post/database/service. Klik Publish, lalu jalankan ulang aplikasi terbaru.

Balasan disimpan sebagai komentar dengan referensi komentar asal dan identitas pengguna yang ditag. Balasan tampil kronologis bersama komentar dengan awalan @nama; belum ada navigasi profil atau notifikasi mention. Suka disimpan pada `posts/{postId}/comments/{commentId}/likes/{uid}`. Bagikan memakai Android Sharesheet untuk teks komentar, tanpa tautan palsu.

```javascript
function validCommentIncrement() {
  let next = request.resource.data;
  let commentPath = /databases/$(database)/documents/posts/$(postId)/comments/$(next.lastCommentId);
  return request.auth != null
    && next.diff(resource.data).affectedKeys().hasOnly(['comment', 'lastCommentId'])
    && next.lastCommentId is string
    && next.lastCommentId.matches('^[a-zA-Z0-9]{20}$')
    && next.comment is int
    && next.comment == resource.data.get('comment', 0) + 1
    && !exists(commentPath)
    && getAfter(commentPath).data.authorId == request.auth.uid
    && getAfter(commentPath).data.createdAt == request.time;
}

allow update: if validCommentIncrement();
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

  allow read: if request.auth != null;

  allow create: if request.auth != null
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
    allow read: if request.auth != null;
    allow create: if request.auth != null
      && request.auth.uid == userId
      && exists(/databases/$(database)/documents/posts/$(postId)/comments/$(commentId))
      && request.resource.data.keys().hasAll(['createdAt'])
      && request.resource.data.keys().hasOnly(['createdAt'])
      && request.resource.data.createdAt is timestamp
      && request.resource.data.createdAt == request.time;
    allow delete: if request.auth != null && request.auth.uid == userId;
    allow update: if false;
  }
}
```

Tidak perlu membuat subkoleksi atau field secara manual. Post lama tanpa field `comment` dimulai dari 0. Aturan ini tidak menghitung ulang komentar historis.

## Pemeriksaan setelah Publish

- Login, buka komentar post: daftar kosong dapat dibaca.
- Kirim komentar: dokumen baru muncul di posts/{postId}/comments dan jumlah naik satu.
- Tutup lalu buka ulang: komentar tetap tampil.
- Coba dari akun lain pada post yang sama.
- Suka lalu batal suka, buka ulang: jumlah dan status tersimpan; akun lain memiliki suka sendiri.
- Balas komentar: banner @nama muncul; Batal menghapus target; setelah terkirim, tag masih terlihat saat dibuka ulang.
- Balas balasan: tag mengikuti penulis balasan yang dipilih.
- Bagikan membuka pilihan aplikasi; membatalkan tidak mengubah database.
- Penulisan suka dengan UID orang lain, pembaruan suka, target balasan di post lain, atau tag dengan authorId palsu harus ditolak.
- Komentar lama tanpa authorId masih dapat dibaca/disukai/dibagikan; tombol Balas dinonaktifkan karena identitas target tidak tersedia.
- Di emulator/Rules Playground: penulisan tanpa login, pemalsuan authorId, perubahan caption lewat update, dan kenaikan jumlah tanpa komentar baru harus ditolak.

## Audit statis

```json
{
  "score": 4,
  "summary": "Aturan mengikat setiap kenaikan jumlah dengan pembuatan satu komentar milik pengguna terautentikasi. Belum diuji dengan Firestore Emulator atau dipublikasikan ke server.",
  "findings": [
    {
      "check": "Authority Source",
      "severity": "minor",
      "issue": "authorName adalah label tampilan dari klien, bukan identitas terverifikasi.",
      "recommendation": "Gunakan authorId sebagai identitas pemilik; jangan gunakan authorName untuk otorisasi."
    }
  ]
}
```
