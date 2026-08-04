import { useEffect, useState } from 'react'
import { apiDelete, apiGet, apiPostJson, apiPutJson } from './api'

// UC-006 notes page: the real post-login destination (replaces Faz 1's
// ProtectedPage placeholder). List / create / edit / delete, scoped to
// whichever user the session cookie identifies.
//
// BR-008 (ownership): this component never filters anything itself - the
// backend only ever returns/accepts the caller's own notes (NoteController
// + NoteRepository), so there is nothing to enforce here beyond rendering
// whatever the API gives back.
//
// A3 (empty title): the backend is the actual enforcement point
// (NoteController.validate). The check below is a client-side convenience
// for instant feedback, not something this code relies on for correctness.
function NotesPage({ username, onLogout, onOpenAccountSettings, onOpenTokenInspector }) {
  const [notes, setNotes] = useState([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState(null)

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [editingId, setEditingId] = useState(null)
  const [formError, setFormError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function loadNotes() {
    setLoading(true)
    try {
      const response = await apiGet('/notes')
      if (response.ok) {
        setNotes(await response.json())
        setListError(null)
      } else {
        setListError('Notlar yüklenemedi.')
      }
    } catch {
      setListError('Notlar yüklenemedi.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadNotes()
  }, [])

  function startEdit(note) {
    setEditingId(note.id)
    setTitle(note.title)
    setContent(note.content ?? '')
    setFormError(null)
  }

  function cancelEdit() {
    setEditingId(null)
    setTitle('')
    setContent('')
    setFormError(null)
  }

  // A1/A2 continue the use case "at step 2" - i.e. back to the list, which
  // is exactly what re-fetching after a successful mutation does.
  async function handleSubmit(event) {
    event.preventDefault()

    if (!title.trim()) {
      setFormError('Başlık zorunludur.')
      return
    }

    setSubmitting(true)
    setFormError(null)
    try {
      const response = editingId
        ? await apiPutJson(`/notes/${editingId}`, { title, content })
        : await apiPostJson('/notes', { title, content })

      if (response.ok) {
        cancelEdit()
        await loadNotes()
        return
      }

      if (response.status === 400) {
        const body = await response.json()
        setFormError(body.error ?? 'Başlık zorunludur.')
      } else {
        setFormError('Not kaydedilemedi, lütfen tekrar deneyin.')
      }
    } catch {
      setFormError('İstek gönderilemedi, lütfen tekrar deneyin.')
    } finally {
      setSubmitting(false)
    }
  }

  // A2: "kullanıcı notu seçer ve silmeyi onaylar" - confirmation happens
  // here before the request goes out.
  async function handleDelete(id) {
    if (!window.confirm('Bu notu silmek istediğinize emin misiniz?')) {
      return
    }
    const response = await apiDelete(`/notes/${id}`)
    if (response.ok) {
      if (editingId === id) cancelEdit()
      await loadNotes()
    }
  }

  return (
    <main>
      <h1>Notlarım</h1>
      <p>
        Hoş geldin, {username}.{' '}
        <button type="button" onClick={onOpenAccountSettings}>
          Hesap Ayarları
        </button>{' '}
        <button type="button" onClick={onOpenTokenInspector}>
          Token Inspector
        </button>{' '}
        <button type="button" onClick={onLogout}>
          Çıkış Yap
        </button>
      </p>

      <form onSubmit={handleSubmit} noValidate>
        <h2>{editingId ? 'Notu Düzenle' : 'Yeni Not'}</h2>
        <div>
          <label htmlFor="title">Başlık</label>
          <input
            id="title"
            type="text"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
        </div>
        <div>
          <label htmlFor="content">İçerik</label>
          <textarea
            id="content"
            value={content}
            onChange={(event) => setContent(event.target.value)}
          />
        </div>
        {formError && <p role="alert">{formError}</p>}
        <button type="submit" disabled={submitting}>
          {editingId ? 'Kaydet' : 'Ekle'}
        </button>
        {editingId && (
          <button type="button" onClick={cancelEdit}>
            Vazgeç
          </button>
        )}
      </form>

      {loading && <p>Yükleniyor...</p>}
      {listError && <p role="alert">{listError}</p>}

      <ul>
        {notes.map((note) => (
          <li key={note.id}>
            <h3>{note.title}</h3>
            <p>{note.content}</p>
            <button type="button" onClick={() => startEdit(note)}>
              Düzenle
            </button>
            <button type="button" onClick={() => handleDelete(note.id)}>
              Sil
            </button>
          </li>
        ))}
      </ul>
    </main>
  )
}

export default NotesPage
