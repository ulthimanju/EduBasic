import React, { useState, useContext } from 'react';
import PropTypes from 'prop-types';
import ThemeContext from '../context/ThemeContext';

/**
 * A reusable icon button component that demonstrates a transition
 * between outlined and filled states, utilizing the app's theme context.
 * @param {Object} props
 * @param {React.ElementType} props.icon - The Lucide-react icon component
 * @param {string} props.label - Text label displayed below the icon
 * @param {boolean} props.isFilled - Controls the active/filled state
 * @param {function} props.onClick - Handler for click events
 */
const ToggleIcon = ({ 
  icon: Icon, 
  label, 
  isFilled, 
  onClick
}) => {
  const [isHovered, setIsHovered] = useState(false);
  const colors = useContext(ThemeContext);

  // Dynamic styles based on state and theme
  const getStyles = () => {
    if (isFilled) {
      return {
        container: {
          backgroundColor: colors.accentDim,
          borderColor: colors.accentSolid,
          boxShadow: 'none',
        },
        icon: {
          color: colors.accentSolid,
        },
        text: {
          color: colors.accentSolid,
        }
      };
    }

    // Default (Outlined) State
    return {
      container: {
        backgroundColor: colors.bgCard,
        borderColor: isHovered ? colors.accentSolid : colors.border,
        boxShadow: isHovered ? colors.shadow : 'none',
        transform: isHovered ? 'translateY(-2px)' : 'translateY(0)',
      },
      icon: {
        color: isHovered ? colors.accentSolid : colors.textMuted,
      },
      text: {
        color: isHovered ? colors.accentSolid : colors.textMuted,
      }
    };
  };

  const currentStyle = getStyles();

  return (
    <button 
      onClick={onClick}
      type="button"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      aria-pressed={isFilled}
      aria-label={label}
      // Base tailwind classes for layout/structure, style prop for dynamic colors
      className="relative group flex flex-col items-center justify-center p-6 rounded-2xl transition-all duration-300 border w-full backdrop-blur-sm"
      style={currentStyle.container}
    >
      <div className={`
        transition-all duration-300 transform
        ${isFilled ? 'scale-110' : 'group-hover:scale-110'}
      `}>
        {Icon && (
          <Icon 
            size={32} 
            className="transition-all duration-300"
            style={currentStyle.icon}
            // The magic happens here: filling the SVG content
            fill={isFilled ? "currentColor" : "none"} 
          />
        )}
      </div>
      
      {label && (
        <span 
          className="mt-3 text-xs font-medium uppercase tracking-wider transition-colors"
          style={currentStyle.text}
        >
          {label}
        </span>
      )}
      
      <div 
        className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity text-[10px] font-mono pointer-events-none"
        style={{ color: colors.textMutedDim }}
      >
        {isFilled ? 'FILLED' : 'OUTLINE'}
      </div>
    </button>
  );
};

ToggleIcon.propTypes = {
  icon: PropTypes.elementType.isRequired,
  label: PropTypes.string,
  isFilled: PropTypes.bool.isRequired,
  onClick: PropTypes.func.isRequired
};

export default ToggleIcon;
