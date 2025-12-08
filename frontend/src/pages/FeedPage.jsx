import React, { useState, useEffect } from 'react';
import { useTheme, Badge, Sidebar, LoadingSpinner } from '../components';
import AppLayout from '../layouts/AppLayout';
import apiFetch, { apiEndpoints } from '../utils/apiClient';

function FeedPageContent({ onLogout, onNavigate, user }) {
  const colors = useTheme();
  const [feedItems, setFeedItems] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [filters, setFilters] = useState({
    announcement: true,
    update: true
  });

  useEffect(() => {
    fetchFeedItems();
  }, []);

  const fetchFeedItems = async () => {
    try {
      setIsLoading(true);
      const response = await apiFetch(apiEndpoints.announcements.list);
      
      if (response.ok) {
        const data = await response.json();
        // Convert timestamps to Date objects for formatting
        const items = data.map(item => ({
          ...item,
          timestamp: new Date(item.createdAt)
        }));
        setFeedItems(items);
      }
    } catch (error) {
      console.error('Error fetching feed items:', error);
      // Fallback to sample data if API fails
      setFeedItems([
        {
          id: 1,
          title: 'Welcome to EduBasic Feed',
          description: 'Stay updated with the latest course updates and announcements',
          timestamp: new Date(Date.now() - 3600000),
          type: 'announcement'
        }
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const formatTime = (date) => {
    if (!date) return 'Unknown';
    
    const now = new Date();
    const timestamp = date instanceof Date ? date : new Date(date);
    const diff = now - timestamp;
    
    // Check if diff is negative (future date) or invalid
    if (isNaN(diff) || diff < 0) return 'Just now';
    
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    return timestamp.toLocaleDateString();
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this announcement?')) {
      return;
    }

    try {
      const response = await apiFetch(apiEndpoints.announcements.delete(id), {
        method: 'DELETE',
      });

      if (response.ok) {
        fetchFeedItems();
      }
    } catch (error) {
      console.error('Error deleting announcement:', error);
    }
  };

  const isAdmin = user?.role === 'ADMIN';

  // Filter feed items based on selected checkboxes
  const filteredFeedItems = feedItems.filter(item => filters[item.type]);

  const handleFilterChange = (type) => {
    setFilters(prev => ({ ...prev, [type]: !prev[type] }));
  };

  return (
    <div className="flex flex-col flex-1 h-full" style={{ background: colors.bgApp }}>

      <div className="flex flex-1">
        {/* Left Sidebar - Filters */}
        <Sidebar
          title="Filters"
          items={[
            {
              id: 'announcement',
              label: 'Announcements',
              checked: filters.announcement,
              onChange: () => handleFilterChange('announcement')
            },
            {
              id: 'update',
              label: 'Updates',
              checked: filters.update,
              onChange: () => handleFilterChange('update')
            }
          ]}
        />

        {/* Main Content */}
        <div className="flex-1 overflow-y-auto p-6">
          <div className="max-w-2xl mx-auto">

          {isLoading ? (
            <div className="text-center py-12 flex justify-center">
              <LoadingSpinner size={48} message="Loading feed..." />
            </div>
          ) : filteredFeedItems.length === 0 ? (
            <div className="text-center py-8">
              <p className="opacity-60" style={{ color: colors.textMain }}>
                No items match the selected filters
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {filteredFeedItems.map((item) => (
                <div
                  key={item.id}
                  className="p-6 rounded-xl border transition-all duration-200 hover:shadow-lg cursor-pointer"
                  style={{
                    background: colors.bgCard,
                    borderColor: colors.border,
                    color: colors.textMain,
                  }}
                >
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold mb-2">{item.title}</h3>
                      <p className="opacity-70 text-sm mb-3">{item.description}</p>
                    </div>
                    <div className="ml-4 flex-shrink-0 flex items-center gap-2">
                      <Badge 
                        label={item.type.charAt(0).toUpperCase() + item.type.slice(1)}
                        active={true}
                      />
                      {isAdmin && (
                        <button
                          onClick={() => handleDelete(item.id)}
                          className="px-2 py-1 rounded text-xs bg-red-600 text-white hover:bg-red-700 transition"
                        >
                          Delete
                        </button>
                      )}
                    </div>
                  </div>
                  <div className="flex gap-4 text-xs opacity-50">
                    <span>{formatTime(item.timestamp)}</span>
                    {item.createdByUsername && (
                      <span>By: {item.createdByUsername}</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function FeedPage({ onLogout, onNavigate, user }) {
  return (
    <AppLayout user={user} onLogout={onLogout} onNavigate={onNavigate} title="Feed">
      <FeedPageContent onLogout={onLogout} onNavigate={onNavigate} user={user} />
    </AppLayout>
  );
}

export default FeedPage;
