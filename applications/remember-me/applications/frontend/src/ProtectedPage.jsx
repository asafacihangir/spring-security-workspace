// Placeholder for the post-login destination. Faz 2 replaces this with the
// real notes page (UC-001 step 6 says "notlar sayfası") - this view exists
// only to prove the session-protected redirect works, per the brief.
function ProtectedPage({ username }) {
  return (
    <main>
      <h1>Giriş başarılı</h1>
      <p>Hoş geldin, {username}.</p>
    </main>
  )
}

export default ProtectedPage
