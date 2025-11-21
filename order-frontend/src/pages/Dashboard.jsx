import { useQuery } from '@tanstack/react-query';
import { ShoppingCart, CheckCircle, AlertCircle, DollarSign } from 'lucide-react';
import StatCard from '../components/common/StatCard';
import { Card, CardHeader, CardTitle, CardContent } from '../components/common/Card';
import StatusBadge from '../components/common/StatusBadge';
import { getOrderStatistics, getAllOrders, getDlqStatistics } from '../services/orderService';

const Dashboard = () => {

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['orderStats'],
    queryFn: getOrderStatistics,
    refetchInterval: 3000,
  });


  const { data: ordersData, isLoading: ordersLoading } = useQuery({
    queryKey: ['recentOrders'],
    queryFn: () => getAllOrders({ page: 0, size: 10 }),
    refetchInterval: 3000,
  });


  const { data: dlqStats } = useQuery({
    queryKey: ['dlqStats'],
    queryFn: getDlqStatistics,
    refetchInterval: 5000,
  });

  const orders = ordersData?.content || [];


  const successRate = stats?.totalOrders > 0 
    ? ((stats.processedOrders / stats.totalOrders) * 100).toFixed(1)
    : 0;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-600 mt-1">Real-time order monitoring and analytics</p>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Total Orders"
          value={statsLoading ? '...' : (stats?.totalOrders || 0).toLocaleString()}
          icon={ShoppingCart}
          trend="up"
          trendValue={`${stats?.ordersLast24Hours || 0} today`}
        />
        
        <StatCard
          title="Success Rate"
          value={statsLoading ? '...' : `${successRate}%`}
          icon={CheckCircle}
        />
        
        <StatCard
          title="Total Revenue"
          value={statsLoading ? '...' : `$${(stats?.totalRevenue || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
          icon={DollarSign}
          trend="up"
        />
        
        <StatCard
          title="Failed Orders"
          value={dlqStats?.pending || 0}
          icon={AlertCircle}
          className={dlqStats?.pending > 0 ? 'border-red-200' : ''}
        />
      </div>

      {/* Recent Orders Table */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Recent Orders</CardTitle>
            <div className="flex items-center space-x-2 text-sm text-gray-500">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
              <span>Live updates</span>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {ordersLoading ? (
            <div className="p-8 text-center text-gray-500">
              Loading orders...
            </div>
          ) : orders.length === 0 ? (
            <div className="p-8 text-center text-gray-500">
              No orders yet. Create your first order!
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Order ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Product
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Price
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Processed At
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {orders.map((order) => (
                    <tr key={order.id} className="hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {order.orderId}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {order.product}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        ${order.price.toFixed(2)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <StatusBadge status={order.status} />
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {new Date(order.processedAt).toLocaleString()}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default Dashboard;