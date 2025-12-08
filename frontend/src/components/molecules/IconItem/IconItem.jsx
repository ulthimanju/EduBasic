import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import ThemeContext from '../../context/ThemeContext';

/**
 * A simple icon button molecule that toggles between filled and outlined states.
 * Supports theme-aware styling through the app's ThemeContext.
 * 
 * @param {Object} props
 * @param {React.ElementType} props.icon - The Lucide-react icon component
 * @param {boolean} props.isFilled - Controls whether the icon is filled or outlined
 * @param {function} props.toggle - Callback function when the button is clicked
 * @param {string} [props.size=32] - Icon size in pixels
 */
const IconItem = ({ 
  icon, 
  isFilled, 
  toggle,
  size = 32 
}) => {
  const colors = useContext(ThemeContext);

  const filledColor = colors?.accentSolid || '#FF375F';
  const outlinedColor = colors?.textMuted || '#1D1D1F';
  const borderColor = colors?.border || 'rgba(0, 0, 0, 0.05)';
  const bgColor = colors?.bgCard || 'rgba(255, 255, 255, 0.6)';

  return (
    <button
      onClick={toggle}
      type="button"
      aria-pressed={isFilled}
      className="flex items-center justify-center p-4 rounded-xl border transition-all duration-300 hover:shadow-md"
      style={{
        borderColor,
        backgroundColor: bgColor,
        boxShadow: 'none'
      }}
    >
      {icon && React.cloneElement(icon, {
        size,
        style: {
          color: isFilled ? filledColor : outlinedColor,
          transition: 'color 300ms'
        },
        fill: isFilled ? 'currentColor' : 'none'
      })}
    </button>
  );
};

IconItem.propTypes = {
  icon: PropTypes.elementType.isRequired,
  isFilled: PropTypes.bool.isRequired,
  toggle: PropTypes.func.isRequired,
  size: PropTypes.number
};

export default IconItem;
