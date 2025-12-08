import React, { useState } from 'react';
import { useTheme } from '../../context/useTheme';

/**
 * Tabs Component - Container for tab navigation and content
 * Manages active tab state and renders tab list and content
 */
export function Tabs({ children, defaultActiveTab = 0, onChange }) {
  const [activeTabIndex, setActiveTabIndex] = useState(defaultActiveTab);
  const colors = useTheme();

  // Filter out non-Tab children
  const tabChildren = React.Children.toArray(children).filter(
    (child) => child?.type?.displayName === 'Tab'
  );

  const handleTabChange = (index) => {
    setActiveTabIndex(index);
    onChange?.(index);
  };

  return (
    <div>
      {/* Tab List */}
      <div
        className="flex border-b overflow-x-auto"
        style={{ borderColor: colors.border }}
      >
        {tabChildren.map((tab, index) => (
          <button
            key={index}
            onClick={() => handleTabChange(index)}
            className={`px-4 py-3 text-sm font-medium whitespace-nowrap transition-all duration-200 flex items-center gap-2 ${
              activeTabIndex === index ? 'border-b-2' : 'opacity-60 hover:opacity-80'
            }`}
            style={{
              color: activeTabIndex === index ? colors.accent : colors.textMain,
              borderBottomColor: activeTabIndex === index ? colors.accent : 'transparent',
            }}
          >
            {tab.props.icon && <span className="text-base">{tab.props.icon}</span>}
            <span>{tab.props.label}</span>
            {tab.props.badge && (
              <span
                className="ml-1 px-2 py-0.5 text-xs rounded-full font-semibold"
                style={{
                  background: colors.accent,
                  color: '#fff',
                }}
              >
                {tab.props.badge}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <div className="mt-4">
        {tabChildren.map((tab, index) => (
          <div
            key={index}
            style={{ display: activeTabIndex === index ? 'block' : 'none' }}
            className="animate-fadeIn"
          >
            {tab.props.children}
          </div>
        ))}
      </div>
    </div>
  );
}

/**
 * Tab Component - Individual tab content
 * Props are consumed by parent Tabs component via tab.props
 */
export function Tab({ children }) {
  return <>{children}</>;
}

Tab.displayName = 'Tab';

export default Tabs;
