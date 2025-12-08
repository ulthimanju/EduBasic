import React, { useState, useMemo, useEffect } from 'react';
import { useTheme, ActionButton, Badge, LoadingSpinner, Toggle } from '../components';
import AppLayout from '../layouts/AppLayout';
import { svgToDataUrlBase64, generateAvatarSVG } from '../utils/avatarGenerator';
import { Mail, MapPin, Link as LinkIcon, Calendar, Edit2, Check, X, Briefcase, Github, Twitter, Linkedin } from 'lucide-react';
import apiFetch, { apiEndpoints } from '../utils/apiClient';

function DashboardContent({ user, onLogout, onNavigate }) {
  const colors = useTheme();
  const [activeTab, setActiveTab] = useState('overview');
  const [isEditing, setIsEditing] = useState(false);
  
  // Profile settings state
  const [profileVisibility, setProfileVisibility] = useState(true);
  const [emailNotifications, setEmailNotifications] = useState(false);
  const [isLoadingSettings, setIsLoadingSettings] = useState(true);
  const [isSavingSettings, setIsSavingSettings] = useState(false);

  // Generate avatar SVG data URL
  const avatarUrl = useMemo(() => {
    if (!user?.username || !user?.email) return null;
    const avatarSvg = generateAvatarSVG({
      username: user.username,
      email: user.email,
      size: 160,
      type: 'organic',
      isRounded: true
    });
    return svgToDataUrlBase64(avatarSvg);
  }, [user?.username, user?.email]);

  // Fetch profile settings on mount
  useEffect(() => {
    const fetchSettings = async () => {
      if (!user?.userId) return;
      
      setIsLoadingSettings(true);
      try {
        const response = await apiFetch(`${apiEndpoints.profile.settings}?userId=${user.userId}`, {
          credentials: 'include'
        });
        if (response.ok) {
          const data = await response.json();
          setProfileVisibility(data.profileVisibility ?? true);
          setEmailNotifications(data.emailNotifications ?? false);
        } else {
          console.error('Failed to fetch profile settings');
        }
      } catch (error) {
        console.error('Error fetching profile settings:', error);
      } finally {
        setIsLoadingSettings(false);
      }
    };

    fetchSettings();
  }, [user?.userId]);

  // Update profile settings handler
  const updateSettings = async (newProfileVisibility, newEmailNotifications) => {
    if (!user?.userId || isSavingSettings) return;
    
    setIsSavingSettings(true);
    try {
      const response = await apiFetch(apiEndpoints.profile.settings, {
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({
          userId: user.userId,
          profileVisibility: newProfileVisibility,
          emailNotifications: newEmailNotifications,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        console.log('Settings updated:', data.message);
      } else {
        console.error('Failed to update settings');
        // Revert on failure
        setProfileVisibility(profileVisibility);
        setEmailNotifications(emailNotifications);
      }
    } catch (error) {
      console.error('Error updating settings:', error);
      // Revert on error
      setProfileVisibility(profileVisibility);
      setEmailNotifications(emailNotifications);
    } finally {
      setIsSavingSettings(false);
    }
  };

  // Toggle handlers
  const handleProfileVisibilityToggle = () => {
    const newValue = !profileVisibility;
    setProfileVisibility(newValue);
    updateSettings(newValue, emailNotifications);
  };

  const handleEmailNotificationsToggle = () => {
    const newValue = !emailNotifications;
    setEmailNotifications(newValue);
    updateSettings(profileVisibility, newValue);
  };

  return (
    <div
      className="flex-1 w-full transition-colors duration-500 overflow-y-auto"
      style={{ 
        background: colors.bgApp, 
        color: colors.textMain,
        scrollBehavior: 'smooth'
      }}
    >
      {/* Custom Scrollbar Styles */}
      <style>{`
        ::-webkit-scrollbar {
          width: 10px;
        }
        ::-webkit-scrollbar-track {
          background: ${colors.bgApp};
        }
        ::-webkit-scrollbar-thumb {
          background: ${colors.border};
          border-radius: 5px;
        }
        ::-webkit-scrollbar-thumb:hover {
          background: ${colors.accent};
        }
      `}</style>

      {/* Main Dashboard Content */}
      <div>
        <div className="max-w-6xl mx-auto p-4 md:p-8 overflow-x-hidden">
        
          <div className="rounded-2xl overflow-hidden" style={{ background: colors.bgCard, border: `1px solid ${colors.border}` }}>
          
            <div className="px-6 md:px-10 pb-8 pt-8">
            
            {/* --- Main Layout --- */}
            <div className="flex flex-col lg:flex-row gap-10">
              
              {/* LEFT SIDE: Avatar & Profile Info */}
              <div className="flex flex-col items-center lg:items-start gap-6">
                
                {/* Avatar */}
                <div className="relative">
                  <div className="w-40 h-40 rounded-full border-4 flex items-center justify-center shadow-md overflow-hidden" style={{ borderColor: colors.bgCard, background: colors.bgCard }}>
                    {avatarUrl ? (
                      <img 
                        src={avatarUrl} 
                        alt={user?.username} 
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <div className="text-5xl font-bold" style={{ color: colors.accent }}>
                        {user?.username?.charAt(0).toUpperCase()}
                      </div>
                    )}
                  </div>
                </div>
                
                {/* Profile Info */}
                <div className="text-center lg:text-left">
                  <h1 className="text-3xl font-bold mb-2">{user?.username}</h1>
                  <p className="text-lg opacity-70 mb-4">{user?.role}</p>
                  
                  {/* Email */}
                  <div className="flex items-center gap-3 text-sm">
                    <Mail size={18} className="opacity-50 shrink-0" />
                    <span className="truncate">{user?.email}</span>
                  </div>
                </div>
              </div>

              {/* RIGHT SIDE: Account Settings */}
              <div className="flex-1">
                <h3 className="text-lg font-bold mb-4">Account Settings</h3>
                {isLoadingSettings ? (
                  <div className="flex items-center justify-center py-8">
                    <LoadingSpinner size={32} />
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-4 rounded-lg" style={{ background: colors.bgApp, border: `1px solid ${colors.border}` }}>
                      <div>
                        <p className="font-medium">Profile Visibility</p>
                        <p className="text-xs opacity-50">Who can see your profile</p>
                      </div>
                      <Toggle
                        active={profileVisibility}
                        onToggle={handleProfileVisibilityToggle}
                      />
                    </div>
                    <div className="flex items-center justify-between p-4 rounded-lg" style={{ background: colors.bgApp, border: `1px solid ${colors.border}` }}>
                      <div>
                        <p className="font-medium">Email Notifications</p>
                        <p className="text-xs opacity-50">Updates and newsletters</p>
                      </div>
                      <Toggle
                        active={emailNotifications}
                        onToggle={handleEmailNotificationsToggle}
                      />
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
      </div>
    </div>
  );
}

function DashboardPage({ user, onLogout, onNavigate }) {
  return (
    <AppLayout user={user} onLogout={onLogout} onNavigate={onNavigate} title="EduBasic Dashboard">
      <DashboardContent user={user} onLogout={onLogout} onNavigate={onNavigate} />
    </AppLayout>
  );
}

export default DashboardPage;
