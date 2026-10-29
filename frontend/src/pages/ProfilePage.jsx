import { useState, useRef, useEffect } from 'react'
import {
  User,
  Mail,
  Phone,
  Shield,
  BadgeCheck,
  Hash,
  Camera,
  CheckCircle2,
  AlertCircle,
  Pencil,
  X,
  Save,
  Lock,
} from 'lucide-react'
import api from '../services/api'
import { useAuth } from '../context/AuthContext'
import { Button, Field, PageHeader, Spinner } from '../components/ui'
import { errorMessage } from '../utils/format'

export default function ProfilePage() {
  const { user: authUser, updateUser } = useAuth()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState({ text: '', type: '' }) // type: 'success' | 'error'
  const fileInputRef = useRef(null)

  // Form state
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [avatar, setAvatar] = useState('')
  const [errors, setErrors] = useState({})

  const fetchProfile = async () => {
    try {
      const res = await api.get('/api/profile')
      setProfile(res.data)
      setName(res.data.name || '')
      setPhone(res.data.phone || '')
      setAvatar(res.data.avatar || '')
    } catch (e) {
      setMsg({ text: errorMessage(e), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProfile()
  }, [])

  const startEdit = () => {
    setName(profile?.name || '')
    setPhone(profile?.phone || '')
    setAvatar(profile?.avatar || '')
    setErrors({})
    setMsg({ text: '', type: '' })
    setEditing(true)
  }

  const cancelEdit = () => {
    setName(profile?.name || '')
    setPhone(profile?.phone || '')
    setAvatar(profile?.avatar || '')
    setErrors({})
    setMsg({ text: '', type: '' })
    setEditing(false)
  }

  const handleImageUpload = (e) => {
    const file = e.target.files?.[0]
    if (!file) return

    if (!file.type.startsWith('image/')) {
      setMsg({ text: 'Please select a valid image file (PNG, JPG, WEBP)', type: 'error' })
      return
    }

    // Limit to 2MB
    if (file.size > 2 * 1024 * 1024) {
      setMsg({ text: 'Image size cannot exceed 2MB', type: 'error' })
      return
    }

    const reader = new FileReader()
    reader.onload = () => {
      setAvatar(reader.result)
      setMsg({ text: '', type: '' })
    }
    reader.readAsDataURL(file)
  }

  const removeAvatar = () => {
    setAvatar('')
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  const validate = () => {
    const errs = {}
    if (!name.trim()) errs.name = 'Full name is required'
    else if (name.trim().length > 100) errs.name = 'Name cannot exceed 100 characters'

    if (phone.trim() && !/^[0-9+\-\s()]{7,20}$/.test(phone.trim())) {
      errs.phone = 'Enter a valid phone number (digits and optional +/hyphens)'
    }
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const saveProfile = async (e) => {
    e.preventDefault()
    if (!validate()) return

    setSaving(true)
    setMsg({ text: '', type: '' })

    try {
      const res = await api.put('/api/profile', {
        name: name.trim(),
        phone: phone.trim(),
        avatar: avatar || null,
      })
      setProfile(res.data)
      updateUser(res.data)
      setEditing(false)
      setMsg({ text: 'Profile updated successfully!', type: 'success' })
    } catch (err) {
      setMsg({ text: errorMessage(err), type: 'error' })
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Spinner />

  const isOwner = (profile?.role === 'ADMIN')
  const displayRole = profile?.displayRole || (isOwner ? 'Owner' : 'Cashier')
  const initials = profile?.name ? profile.name.split(' ').map((n) => n[0]).join('').slice(0, 2).toUpperCase() : 'U'

  return (
    <>
      <PageHeader
        title="My Profile"
        subtitle="Manage your personal information, role details, and account preferences"
      >
        {!editing && (
          <Button onClick={startEdit}>
            <Pencil size={16} /> Edit Profile
          </Button>
        )}
      </PageHeader>

      {msg.text && (
        <div
          className={msg.type === 'success' ? 'alert-success' : 'alert-error'}
          style={{ marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          {msg.type === 'success' ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
          <span>{msg.text}</span>
        </div>
      )}

      <div className="profile-container">
        {/* Profile Card */}
        <div className="card profile-header-card">
          <div className="profile-avatar-section">
            <div className="profile-avatar-wrap">
              {(editing ? avatar : profile?.avatar) ? (
                <img
                  src={editing ? avatar : profile.avatar}
                  alt={profile?.name}
                  className="profile-avatar-large"
                />
              ) : (
                <div className="profile-avatar-large-initials">{initials}</div>
              )}

              {editing && (
                <div className="avatar-edit-overlay">
                  <input
                    type="file"
                    ref={fileInputRef}
                    onChange={handleImageUpload}
                    accept="image/*"
                    style={{ display: 'none' }}
                    id="profile-avatar-input"
                  />
                  <label htmlFor="profile-avatar-input" className="avatar-upload-btn" title="Upload new photo">
                    <Camera size={16} />
                  </label>
                  {avatar && (
                    <button
                      type="button"
                      className="avatar-remove-btn"
                      onClick={removeAvatar}
                      title="Remove photo"
                    >
                      <X size={15} />
                    </button>
                  )}
                </div>
              )}
            </div>

            <div className="profile-headline">
              <h2>{profile?.name}</h2>
              <div className="profile-tags-row">
                <span className="badge badge-indigo">
                  <Shield size={13} style={{ marginRight: 4, verticalAlign: -1 }} />
                  {displayRole}
                </span>
                {!isOwner && profile?.employeeId && (
                  <span className="badge badge-gray">
                    <Hash size={13} style={{ marginRight: 4, verticalAlign: -1 }} />
                    {profile.employeeId}
                  </span>
                )}
                <span className="badge badge-green">
                  <BadgeCheck size={13} style={{ marginRight: 4, verticalAlign: -1 }} />
                  Active Account
                </span>
              </div>
              {editing && (
                <p className="muted" style={{ fontSize: '0.8rem', marginTop: '0.4rem', marginBottom: 0 }}>
                  Click the camera icon to upload a PNG or JPG photo (Max 2MB).
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Details Form Card */}
        <div className="card" style={{ marginTop: '1.25rem' }}>
          <div className="card-head" style={{ marginBottom: '1.25rem' }}>
            <h2 style={{ fontSize: '1.1rem' }}>Account Information</h2>
            {editing && <span className="badge badge-indigo">Edit Mode Active</span>}
          </div>

          {editing ? (
            <form onSubmit={saveProfile} noValidate>
              <div className="grid-2">
                <Field label="Full Name" error={errors.name} required>
                  <input
                    className="input"
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Enter full name"
                    autoFocus
                  />
                </Field>

                <Field label="Mobile Number" error={errors.phone}>
                  <input
                    className="input"
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="+91 98765 43210"
                  />
                </Field>
              </div>

              <div className="grid-2" style={{ marginTop: '0.5rem' }}>
                <Field label="Email Address (Read-only)">
                  <div className="readonly-field-box">
                    <Mail size={16} className="muted" />
                    <span>{profile?.email}</span>
                    <span className="readonly-pill"><Lock size={12} /> Fixed</span>
                  </div>
                </Field>

                <Field label="Assigned Role (Read-only)">
                  <div className="readonly-field-box">
                    <Shield size={16} className="muted" />
                    <span>{displayRole}</span>
                    <span className="readonly-pill"><Lock size={12} /> Fixed</span>
                  </div>
                </Field>
              </div>

              {!isOwner && profile?.employeeId && (
                <div style={{ marginTop: '0.5rem', maxWidth: '50%' }}>
                  <Field label="Employee ID (Read-only)">
                    <div className="readonly-field-box">
                      <Hash size={16} className="muted" />
                      <span>{profile.employeeId}</span>
                      <span className="readonly-pill"><Lock size={12} /> Fixed</span>
                    </div>
                  </Field>
                </div>
              )}

              <div className="form-actions" style={{ marginTop: '1.75rem', display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                <Button type="button" variant="ghost" onClick={cancelEdit} disabled={saving}>
                  <X size={16} /> Cancel
                </Button>
                <Button type="submit" loading={saving}>
                  <Save size={16} /> Save Changes
                </Button>
              </div>
            </form>
          ) : (
            <div className="profile-details-grid">
              <div className="profile-detail-item">
                <div className="detail-icon"><User size={18} /></div>
                <div className="detail-content">
                  <div className="detail-label">Full Name</div>
                  <div className="detail-value">{profile?.name || '—'}</div>
                </div>
              </div>

              <div className="profile-detail-item">
                <div className="detail-icon"><Mail size={18} /></div>
                <div className="detail-content">
                  <div className="detail-label">Email Address</div>
                  <div className="detail-value">{profile?.email || '—'}</div>
                </div>
              </div>

              <div className="profile-detail-item">
                <div className="detail-icon"><Phone size={18} /></div>
                <div className="detail-content">
                  <div className="detail-label">Mobile Number</div>
                  <div className="detail-value">
                    {profile?.phone ? (
                      profile.phone
                    ) : (
                      <span className="muted" style={{ fontStyle: 'italic' }}>Not provided yet</span>
                    )}
                  </div>
                </div>
              </div>

              <div className="profile-detail-item">
                <div className="detail-icon"><Shield size={18} /></div>
                <div className="detail-content">
                  <div className="detail-label">Role</div>
                  <div className="detail-value">
                    <span className="badge badge-indigo">{displayRole}</span>
                  </div>
                </div>
              </div>

              {!isOwner && profile?.employeeId && (
                <div className="profile-detail-item">
                  <div className="detail-icon"><Hash size={18} /></div>
                  <div className="detail-content">
                    <div className="detail-label">Employee ID</div>
                    <div className="detail-value">
                      <span className="badge badge-gray">{profile.employeeId}</span>
                    </div>
                  </div>
                </div>
              )}

              <div className="profile-detail-item">
                <div className="detail-icon"><BadgeCheck size={18} /></div>
                <div className="detail-content">
                  <div className="detail-label">Status</div>
                  <div className="detail-value">
                    <span className="badge badge-green">Active</span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  )
}
