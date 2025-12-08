import React, { useState } from 'react';
import {
  useTheme,
  ActionButton,
} from '../components';
import apiFetch, { apiEndpoints } from '../utils/apiClient';
import AppLayout from '../layouts/AppLayout';
import logoSvg from '../assets/logo.svg';

function UploadContentPageContent({ onNavigate, onLogout, user }) {
  const colors = useTheme();
  const [selectedFile, setSelectedFile] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [uploading, setUploading] = useState(false);
  const [fileInfo, setFileInfo] = useState(null);
  const [showGuidelines, setShowGuidelines] = useState(false);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    
    if (!file) {
      setSelectedFile(null);
      setFileInfo(null);
      return;
    }

    // Check if file is JSON
    if (file.type !== 'application/json' && !file.name.endsWith('.json')) {
      setError('Please select a valid JSON file');
      setSelectedFile(null);
      setFileInfo(null);
      return;
    }

    setSelectedFile(file);
    setError('');
    setSuccess('');
    setFileInfo({
      name: file.name,
      size: (file.size / 1024).toFixed(2), // Convert to KB
      type: file.type,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!selectedFile) {
      setError('Please select a file');
      return;
    }

    setUploading(true);
    setError('');
    setSuccess('');

    try {
      // Prepare FormData
      const formData = new FormData();
      formData.append('file', selectedFile);
      
      // Send to backend with Authorization header
      const response = await apiFetch(apiEndpoints.upload, {
        method: 'POST',
        body: formData,
      });

      const data = await response.json().catch(() => null);

      if (response.ok) {
        setSuccess(`File uploaded successfully! Course: ${data?.courseTitle || 'Unknown'}`);
        setSelectedFile(null);
        setFileInfo(null);
        // Reset file input
        const fileInput = document.getElementById('fileInput');
        if (fileInput) fileInput.value = '';
      } else {
        const errorMsg = data?.message || `Upload failed with status ${response.status}`;
        setError(errorMsg);
        console.error('Upload failed:', data);
      }
    } catch (error) {
      console.error('Upload error:', error);
      setError(`Failed to connect to server: ${error.message}`);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div
      className="min-h-screen w-full transition-colors duration-500"
      style={{ background: colors.bgApp, color: colors.textMain }}
    >


      {/* Two Column Layout */}
      <div className="max-w-7xl mx-auto p-4">
        {/* Left Column - Guidelines (Hidden by default, shows on question icon click) */}
        {showGuidelines && (
          <div
            className="rounded-[28px] p-8 backdrop-blur-xl border shadow-2xl h-fit animate-in fade-in slide-in-from-left duration-300"
            style={{
              background: colors.bgPanel,
              borderColor: colors.border,
              boxShadow: colors.shadow,
            }}
          >
            <button
              onClick={() => setShowGuidelines(false)}
              className="w-full flex items-center justify-between mb-6 hover:opacity-80 transition-opacity"
            >
              <h2 className="text-2xl font-bold">Upload Guidelines</h2>
              <span className="text-2xl">✕</span>
            </button>

            {/* File Format Guidelines */}
            <div className="space-y-4 mb-6">
              <div className="flex gap-4">
                <div
                  className="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 text-xl"
                  style={{ background: colors.bgInput }}
                >
                  📄
                </div>
                <div>
                  <h3 className="font-semibold mb-1">JSON Format</h3>
                  <p className="text-sm opacity-70">
                    Your file must be in valid JSON format with proper structure
                  </p>
                </div>
              </div>

              <div className="flex gap-4">
                <div
                  className="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 text-xl"
                  style={{ background: colors.bgInput }}
                >
                  📊
                </div>
                <div>
                  <h3 className="font-semibold mb-1">Data Structure</h3>
                  <p className="text-sm opacity-70">
                    Supports both objects and arrays. Ensure your data has consistent fields
                  </p>
                </div>
              </div>

              <div className="flex gap-4">
                <div
                  className="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 text-xl"
                  style={{ background: colors.bgInput }}
                >
                  💾
                </div>
                <div>
                  <h3 className="font-semibold mb-1">File Size</h3>
                  <p className="text-sm opacity-70">
                    No file size limits. Process files of any size efficiently
                  </p>
                </div>
              </div>

              <div className="flex gap-4">
                <div
                  className="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 text-xl"
                  style={{ background: colors.bgInput }}
                >
                  🔒
                </div>
                <div>
                  <h3 className="font-semibold mb-1">Security</h3>
                  <p className="text-sm opacity-70">
                    All uploaded data is securely processed and stored
                  </p>
                </div>
              </div>
            </div>

            {/* Best Practices */}
            <div className="pt-6 border-t" style={{ borderColor: colors.border }}>
              <h3 className="font-semibold mb-3 text-sm">Best Practices</h3>
              <ul className="text-sm opacity-70 space-y-2">
                <li>✓ Validate your JSON before uploading</li>
                <li>✓ Use consistent naming conventions</li>
                <li>✓ Include all required fields</li>
                <li>✓ Test with sample data first</li>
                <li>✓ Keep field types consistent</li>
              </ul>
            </div>
          </div>
        )}

        {/* Right Column - Upload Form */}
        <div
          className="rounded-[28px] p-8 backdrop-blur-xl border shadow-2xl"
          style={{
            background: colors.bgPanel,
            borderColor: colors.border,
            boxShadow: colors.shadow,
          }}
        >
          {/* Header with Question Icon */}
          <div className="flex items-start justify-between mb-8">
            <div>
              <h1 className="text-2xl font-bold mb-2">Upload Your Content</h1>
              <p className="text-sm opacity-60">
                Select a JSON file to upload and process
              </p>
            </div>
            <button
              onClick={() => setShowGuidelines(true)}
              className="w-10 h-10 rounded-full flex items-center justify-center text-lg flex-shrink-0 cursor-pointer hover:opacity-80 transition-opacity"
              style={{ background: colors.bgInput }}
              title="Need help? Check the guidelines"
            >
              ❓
            </button>
          </div>

          {/* Error Message */}
          {error && (
            <div
              className="mb-6 p-4 rounded-xl text-sm font-medium"
              style={{
                background: colors.errorBg,
                color: colors.error,
              }}
            >
              {error}
            </div>
          )}

          {/* Success Message */}
          {success && (
            <div
              className="mb-6 p-4 rounded-xl text-sm font-medium"
              style={{
                background: colors.successBg,
                color: colors.success,
              }}
            >
              {success}
            </div>
          )}

          {/* Upload Form */}
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* File Input */}
            <div>
              <label
                htmlFor="fileInput"
                className="block text-sm font-semibold mb-3 opacity-80"
              >
                Select JSON File
              </label>
              <label
                htmlFor="fileInput"
                className="relative border-2 border-dashed rounded-2xl p-8 text-center cursor-pointer transition-all hover:opacity-80 block"
                style={{
                  borderColor: colors.accentSolid,
                  background: colors.bgInput,
                }}
              >
                <input
                  id="fileInput"
                  type="file"
                  accept=".json,application/json"
                  onChange={handleFileChange}
                  className="hidden"
                />
                <div className="text-4xl mb-3">📄</div>
                <div className="font-semibold mb-1">
                  {selectedFile ? 'File Selected' : 'Choose a file or drag and drop'}
                </div>
                <div className="text-xs opacity-60">
                  {selectedFile
                    ? selectedFile.name
                    : 'JSON files up to any size'}
                </div>
              </label>
            </div>

            {/* File Info */}
            {fileInfo && (
              <div
                className="p-4 rounded-2xl space-y-2"
                style={{ background: colors.bgCard }}
              >
                <div className="flex justify-between">
                  <span className="text-xs uppercase font-semibold opacity-50">
                    File Name
                  </span>
                  <span className="font-semibold text-sm">{fileInfo.name}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-xs uppercase font-semibold opacity-50">
                    File Size
                  </span>
                  <span className="font-semibold text-sm">{fileInfo.size} KB</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-xs uppercase font-semibold opacity-50">
                    File Type
                  </span>
                  <span className="font-semibold text-sm">{fileInfo.type}</span>
                </div>
              </div>
            )}

            {/* Submit Button */}
            <div className="flex justify-center pt-4">
              <ActionButton
                label={uploading ? 'Uploading...' : 'Upload File'}
                onClick={() => {}}
                className="px-12"
                disabled={!selectedFile || uploading}
              />
            </div>

            {/* Info Note */}
            <div
              className="p-4 rounded-2xl text-sm"
              style={{ background: colors.bgCard }}
            >
              <p className="opacity-70">
                <strong>Note:</strong> Make sure your JSON file is valid before uploading. 
                The system will validate the format automatically.
              </p>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

function UploadContentPage({ onNavigate, onLogout, user }) {
  return (
    <AppLayout user={user} onLogout={onLogout} onNavigate={onNavigate} title="Upload Content">
      <UploadContentPageContent onNavigate={onNavigate} onLogout={onLogout} user={user} />
    </AppLayout>
  );
}

export default UploadContentPage;
