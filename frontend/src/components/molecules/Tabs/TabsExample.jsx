import React from 'react';
import { Tabs, Tab, useTheme } from '../components';

/**
 * Example usage of the Tabs component
 */
export function TabsExample() {
  const colors = useTheme();

  return (
    <div style={{ padding: '20px', background: colors.bgApp }}>
      <h2 className="text-2xl font-bold mb-6">Tab Component Examples</h2>

      {/* Example 1: Basic Tabs */}
      <div className="mb-8">
        <h3 className="text-lg font-semibold mb-4">Basic Tabs</h3>
        <Tabs defaultActiveTab={0}>
          <Tab label="Code">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>Code content here...</p>
            </div>
          </Tab>
          <Tab label="Output">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>Output content here...</p>
            </div>
          </Tab>
          <Tab label="Visualization">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>Visualization content here...</p>
            </div>
          </Tab>
        </Tabs>
      </div>

      {/* Example 2: Tabs with Icons */}
      <div className="mb-8">
        <h3 className="text-lg font-semibold mb-4">Tabs with Icons</h3>
        <Tabs defaultActiveTab={0}>
          <Tab label="Overview" icon="📊">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>Overview content...</p>
            </div>
          </Tab>
          <Tab label="Details" icon="📝">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>Details content...</p>
            </div>
          </Tab>
          <Tab label="Settings" icon="⚙️">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>Settings content...</p>
            </div>
          </Tab>
        </Tabs>
      </div>

      {/* Example 3: Tabs with Badges */}
      <div className="mb-8">
        <h3 className="text-lg font-semibold mb-4">Tabs with Badges</h3>
        <Tabs defaultActiveTab={0}>
          <Tab label="Inbox" badge="5">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>You have 5 new messages</p>
            </div>
          </Tab>
          <Tab label="Sent" badge="2">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>2 unsent messages</p>
            </div>
          </Tab>
          <Tab label="Drafts">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>No drafts</p>
            </div>
          </Tab>
        </Tabs>
      </div>

      {/* Example 4: Tabs with onChange Handler */}
      <div>
        <h3 className="text-lg font-semibold mb-4">Tabs with Callback</h3>
        <Tabs 
          defaultActiveTab={0}
          onChange={(index) => console.log('Active tab:', index)}
        >
          <Tab label="Tab 1" icon="1️⃣">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>First tab content</p>
            </div>
          </Tab>
          <Tab label="Tab 2" icon="2️⃣">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>Second tab content</p>
            </div>
          </Tab>
          <Tab label="Tab 3" icon="3️⃣">
            <div className="p-4" style={{ background: colors.bgCard }}>
              <p>Third tab content</p>
            </div>
          </Tab>
        </Tabs>
      </div>
    </div>
  );
}

export default TabsExample;
