import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Card, CardHeader, CardTitle, CardContent } from '../components/common/Card';
import StatusBadge from '../components/common/StatusBadge';
import { 
  getFailedOrders, 
  getDlqStatistics, 
  reprocessOrder, 
  reprocessAllPending, 
  discardOrder 
} from '../services/orderService';
import toast from 'react-hot-toast';
import { RefreshCw, Trash2, AlertCircle, CheckCircle, XCircle } from 'lucide-react';

const DLQ = () => {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState('all');
  const [selectedOrder, setSelectedOrder] = useState(null);


  const { data: failedOrders, isLoading } = useQuery({
    queryKey: ['failedOrders', filter],
    queryFn: () => getFailedOrders(filter === 'all' ? null : filter),
    refetchInterval: 5000,
  });


  const { data: stats } = useQuery({
    queryKey: ['dlqStats'],
    queryFn: getDlqStatistics,
    refetchInterval: 5000,
  });


  const reprocessMutation = useMutation({
    mutationFn: (id) => reprocessOrder(id, 'admin'),
    onSuccess: () => {
      toast.success(' Order reprocessed successfully!');
      queryClient.invalidateQueries(['failedOrders']);
      queryClient.invalidateQueries(['dlqStats']);
    },
    onError: (error) => {
      toast.error(` Reprocess failed: ${error.message}`);
    },
  });


  const reprocessAllMutation = useMutation({
    mutationFn: () => reprocessAllPending('admin'),
    onSuccess: (data) => {
      toast.success(` Reprocessed ${data.success} orders!`);
      if (data.failed > 0) {
        toast.error(` ${data.failed} orders failed to reprocess`);
      }
      queryClient.invalidateQueries(['failedOrders']);
      queryClient.invalidateQueries(['dlqStats']);
    },
    onError: (error) => {
      toast.error(`Bulk reprocess failed: ${error.message}`);
    },
  });

  // Discard order mutation
  const discardMutation = useMutation({
    mutationFn: discardOrder,
    onSuccess: () => {
      toast.success('️ Order discarded');
      queryClient.invalidateQueries(['failedOrders']);
      queryClient.invalidateQueries(['dlqStats']);
    },
    onError: (error) => {
      toast.error(` Discard failed: ${error.message}`);
    },
  });

  const handleReprocess = (id) => {
    if (window.confirm('Reprocess this order?')) {
      reprocessMutation.mutate(id);
    }
  };

  const handleDiscard = (id) => {
    if (window.confirm('Discard this order? This action cannot be undone.')) {
      discardMutation.mutate(id);
    }
  };

  const handleReprocessAll = () => {
    if (window.confirm(`Reprocess all ${stats?.pending || 0} pending orders?`)) {
      reprocessAllMutation.mutate();
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dead Letter Queue</h1>
          <p className="text-gray-600 mt-1">Manage failed orders and retry processing</p>
        </div>
        <button
          onClick={handleReprocessAll}
          disabled={reprocessAllMutation.isPending || (stats?.pending || 0) === 0}
          className="flex items-center space-x-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Reprocess All Pending</span>
        </button>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-gray-600">Total Failed</p>
                <p className="text-2xl font-bold text-gray-900">{stats?.total || 0}</p>
              </div>
              <AlertCircle className="w-8 h-8 text-red-500" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-gray-600">Pending</p>
                <p className="text-2xl font-bold text-yellow-600">{stats?.pending || 0}</p>
              </div>
              <AlertCircle className="w-8 h-8 text-yellow-500" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-gray-600">Temporary</p>
                <p className="text-2xl font-bold text-orange-600">{stats?.temporary || 0}</p>
              </div>
              <RefreshCw className="w-8 h-8 text-orange-500" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-gray-600">Permanent</p>
                <p className="text-2xl font-bold text-red-600">{stats?.permanent || 0}</p>
              </div>
              <XCircle className="w-8 h-8 text-red-500" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-gray-600">Reprocessed</p>
                <p className="text-2xl font-bold text-green-600">{stats?.reprocessed || 0}</p>
              </div>
              <CheckCircle className="w-8 h-8 text-green-500" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filter Tabs */}
      <Card>
        <CardContent className="p-2">
          <div className="flex space-x-2">
            {['all', 'PENDING', 'REPROCESSED', 'DISCARDED'].map((status) => (
              <button
                key={status}
                onClick={() => setFilter(status)}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  filter === status
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {status === 'all' ? 'All' : status}
              </button>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Failed Orders Table */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Failed Orders</CardTitle>
            <div className="flex items-center space-x-2 text-sm text-gray-500">
              <div className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></div>
              <span>Auto-refresh: 5s</span>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-8 text-center">
              <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-red-600"></div>
              <p className="text-gray-600 mt-2">Loading failed orders...</p>
            </div>
          ) : !failedOrders || failedOrders.length === 0 ? (
            <div className="p-8 text-center text-gray-500">
              {filter === 'all' ? 'No failed orders' : `No ${filter.toLowerCase()} orders`}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Order ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Product
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Failure Type
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Category
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Error
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Retries
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {failedOrders.map((order) => (
                    <tr key={order.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        #{order.id}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                        {order.orderId}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {order.product}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <StatusBadge status={order.failureType} />
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className="text-xs bg-gray-100 px-2 py-1 rounded">
                          {order.failureCategory}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-600 max-w-xs truncate">
                        {order.errorMessage}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {order.retryCount}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <StatusBadge status={order.status} />
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm">
                        {order.status === 'PENDING' && (
                          <div className="flex space-x-2">
                            <button
                              onClick={() => handleReprocess(order.id)}
                              disabled={reprocessMutation.isPending}
                              className="text-green-600 hover:text-green-800 disabled:opacity-50"
                              title="Reprocess"
                            >
                              <RefreshCw className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => handleDiscard(order.id)}
                              disabled={discardMutation.isPending}
                              className="text-red-600 hover:text-red-800 disabled:opacity-50"
                              title="Discard"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        )}
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

export default DLQ;