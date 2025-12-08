import React, { useState, useEffect } from 'react';
import { useTheme, Badge, InputField, SelectField, Table, ActionButton, LoadingSpinner } from '../components';
import apiFetch, { apiEndpoints, getAuthToken } from '../utils/apiClient';
import AppLayout from '../layouts/AppLayout';

function AdminPageContent({ onNavigate, user }) {
  const colors = useTheme();
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    type: 'announcement'
  });
  const [editingId, setEditingId] = useState(null);
  const [announcements, setAnnouncements] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // Fetch announcements only when user changes, using an effect-safe pattern
  useEffect(() => {
    let isMounted = true;
    const fetchAnnouncements = async () => {
      setIsLoading(true);
      try {
        const response = await apiFetch(apiEndpoints.announcements.list, {
          headers: {
            'Content-Type': 'application/json'
          }
        });
        if (response.ok) {
          const data = await response.json();
          if (isMounted) setAnnouncements(data);
        }
      } catch (err) {
        console.error('Failed to fetch announcements', err);
      }
      if (isMounted) setIsLoading(false);
    };
    console.log('Admin Page - User object:', user);
    fetchAnnouncements();
    return () => { isMounted = false; };
  }, [user]);

  // Handler for form input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  // Handler for form submit
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!user || !user.userId) {
      // Error removed (was unused)
      return;
    }
    try {
      const token = getAuthToken();
      const params = new URLSearchParams({
        title: formData.title,
        description: formData.description,
        type: formData.type,
        userId: user.userId
      });
      const url = editingId
        ? `http://localhost:8080/api/announcements/${editingId}?${params}`
        : `http://localhost:8080/api/announcements?${params}`;
      const method = editingId ? 'PUT' : 'POST';
      const response = await fetch(url, {
        method,
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      if (response.ok) {
        setFormData({ title: '', description: '', type: 'announcement' });
        setEditingId(null);
        // Refresh announcements after submit
        setIsLoading(true);
        const res = await apiFetch(apiEndpoints.announcements.list, {
          headers: {
            'Content-Type': 'application/json'
          }
        });
        if (res.ok) {
          const data = await res.json();
          setAnnouncements(data);
        }
        setIsLoading(false);
      }
    } catch (err) {
      console.error('Failed to submit announcement', err);
    }
  };

  const handleEdit = (announcement) => {
    setFormData({
      title: announcement.title,
      description: announcement.description,
      type: announcement.type
    });
    setEditingId(announcement.id);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this announcement?')) {
      return;
    }

    try {
      const response = await apiFetch(apiEndpoints.announcements.delete(id), {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        console.log(`🗑️ User "${user?.username}" deleted announcement (ID: ${id})`);
        // Refresh announcements after delete
        setIsLoading(true);
        const res = await apiFetch(apiEndpoints.announcements.list, {
          headers: {
            'Content-Type': 'application/json'
          }
        });
        if (res.ok) {
          const data = await res.json();
          setAnnouncements(data);
        }
        setIsLoading(false);
      }
    } catch (err) {
      console.error('Failed to delete announcement', err);
    }
  };

  const handleCancel = () => {
    setFormData({ title: '', description: '', type: 'announcement' });
    setEditingId(null);
  };

  const formatTime = (timestamp) => {
    if (!timestamp) return 'Unknown';
    
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;
    
    // Check if diff is negative (future date) or invalid
    if (isNaN(diff) || diff < 0) return 'Just now';
    
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    return date.toLocaleDateString();
  };

  return (
    <div className="flex-1 overflow-y-auto p-6" style={{ background: colors.bgApp }}>
        <div className="max-w-4xl mx-auto">
          <h1 className="text-3xl font-bold mb-2" style={{ color: colors.textMain }}>
            Announcement Management
          </h1>
          <p className="text-sm opacity-60 mb-8" style={{ color: colors.textMain }}>
            Create, edit, and manage announcements
          </p>
          {/* Create/Edit Form */}
          <div className="mb-8 p-6 rounded-xl" style={{ background: colors.bgCard, borderColor: colors.border }}>
            <form onSubmit={handleSubmit} className="space-y-4">
              <InputField
                label="Title *"
                placeholder="Announcement title"
                value={formData.title}
                onChange={(e) => setFormData(prev => ({ ...prev, title: e.target.value }))}
              />
              <label className="flex flex-col gap-1.5">
                <span className="text-xs font-semibold ml-1 opacity-60" style={{ color: colors.textMain }}>
                  Description *
                </span>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  required
                  placeholder="Announcement description"
                  rows="4"
                  className="px-4 py-3 rounded-xl outline-none focus:ring-2 focus:ring-opacity-50 transition-all font-medium text-sm resize-none"
                  style={{
                    backgroundColor: colors.bgInput,
                    color: colors.textMain,
                    '--tw-ring-color': colors.accentSolid,
                  }}
                />
              </label>
              <SelectField
                label="Type"
                value={formData.type}
                onChange={handleInputChange}
                options={[
                  { value: 'announcement', label: 'Announcement' },
                  { value: 'update', label: 'Update' }
                ]}
                className="w-full"
              />
              <div className="flex gap-3 pt-4">
                <button
                  type="submit"
                  className="px-6 py-3 rounded-xl text-sm font-bold text-white shadow-lg active:scale-[0.97] transition-all hover:brightness-110"
                  style={{ background: colors.accent }}
                >
                  {editingId ? 'Update' : 'Create'}
                </button>
                {editingId && (
                  <button
                    type="button"
                    onClick={handleCancel}
                    className="px-6 py-3 rounded-xl text-sm font-semibold active:scale-[0.97] transition-all hover:bg-black/5 dark:hover:bg-white/10"
                    style={{ color: colors.textMain }}
                  >
                    Cancel
                  </button>
                )}
              </div>
            </form>
          </div>
          {/* Announcements List */}
          <div>
            <h2 className="text-xl font-semibold mb-4" style={{ color: colors.textMain }}>
              All Announcements ({announcements.length})
            </h2>
            {isLoading ? (
              <div className="text-center py-12 flex justify-center">
                <LoadingSpinner size={48} message="Loading announcements..." />
              </div>
            ) : (
              <Table
                data={announcements}
                columns={[
                  { key: 'title', label: 'Title' },
                  {
                    key: 'description',
                    label: 'Description',
                    render: (value) => (
                      <div className="text-sm opacity-70 max-w-md truncate">{value}</div>
                    )
                  },
                  {
                    key: 'type',
                    label: 'Type',
                    render: (value) => (
                      <Badge
                        label={value.charAt(0).toUpperCase() + value.slice(1)}
                        active={true}
                      />
                    )
                  },
                  {
                    key: 'createdByUsername',
                    label: 'Created By',
                    render: (value) => <span className="text-sm opacity-70">{value || 'Unknown'}</span>
                  },
                  {
                    key: 'createdAt',
                    label: 'Created',
                    render: (value) => <span className="text-sm opacity-70">{formatTime(value)}</span>
                  },
                  {
                    key: 'id',
                    label: 'Actions',
                    render: (value, row) => (
                      <div className="flex gap-2 justify-center">
                        <button
                          onClick={() => handleEdit(row)}
                          className="px-4 py-2 rounded-lg text-sm font-semibold text-white transition-all hover:brightness-110 active:scale-[0.98]"
                          style={{ background: colors.accent }}
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDelete(row.id)}
                          className="px-4 py-2 rounded-lg text-sm font-semibold transition-all hover:opacity-80 active:scale-[0.98]"
                          style={{ background: colors.bgPanel, color: colors.textMain, border: `1px solid ${colors.border}` }}
                        >
                          Delete
                        </button>
                      </div>
                    )
                  }
                ]}
                keyField="id"
                emptyState={
                  <div className="text-center py-8 p-6 rounded-xl" style={{ background: colors.bgCard }}>
                    <p className="opacity-60" style={{ color: colors.textMain }}>No announcements yet</p>
                  </div>
                }
              />
            )}
          </div>
        </div>
      </div>
  );
}

function AdminPage({ onNavigate, user }) {
  return (
    <AppLayout user={user} onLogout={() => {}} onNavigate={onNavigate} title="Admin Panel">
      <AdminPageContent onNavigate={onNavigate} user={user} />
    </AppLayout>
  );
}

export default AdminPage;
