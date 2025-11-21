// src/components/common/StatCard.jsx
import { TrendingUp, TrendingDown } from 'lucide-react';

const StatCard = ({ 
  title, 
  value, 
  icon: Icon, 
  trend, 
  trendValue, 
  className = '' 
}) => {
  return (
    <div className={`bg-white rounded-lg border border-gray-200 shadow-sm ${className}`}>
      <div className="p-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <p className="text-sm font-medium text-gray-600">{title}</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
          </div>
          {Icon && (
            <div className="p-3 bg-blue-50 rounded-lg">
              <Icon className="w-6 h-6 text-blue-600" />
            </div>
          )}
        </div>
        
        {(trend || trendValue) && (
          <div className="flex items-center text-sm">
            {trend === 'up' && (
              <TrendingUp className="w-4 h-4 text-green-500 mr-1" />
            )}
            {trend === 'down' && (
              <TrendingDown className="w-4 h-4 text-red-500 mr-1" />
            )}
            <span className={`font-medium ${
              trend === 'up' ? 'text-green-600' : 
              trend === 'down' ? 'text-red-600' : 
              'text-gray-600'
            }`}>
              {trendValue}
            </span>
          </div>
        )}
      </div>
    </div>
  );
};

export default StatCard;