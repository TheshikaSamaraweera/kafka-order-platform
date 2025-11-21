// src/pages/Producer.jsx
import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Card, CardHeader, CardTitle, CardContent } from '../components/common/Card';
import { createOrder, createRandomOrder } from '../services/orderService';
import toast from 'react-hot-toast';
import { PlusCircle, Shuffle, Zap } from 'lucide-react';

const Producer = () => {
  const [formData, setFormData] = useState({
    orderId: '',
    product: '',
    price: '',
  });

  const [errorSimulation, setErrorSimulation] = useState('');


  const createOrderMutation = useMutation({
    mutationFn: createOrder,
    onSuccess: () => {
      toast.success(' Order created successfully!');
      setFormData({ orderId: '', product: '', price: '' });
    },
    onError: (error) => {
      toast.error(` Failed to create order: ${error.message}`);
    },
  });


  const randomOrderMutation = useMutation({
    mutationFn: createRandomOrder,
    onSuccess: (data) => {
      toast.success(` Random order created: ${data}`);
    },
    onError: (error) => {
      toast.error(` Failed: ${error.message}`);
    },
  });

  // Mutation for bulk orders
  const bulkOrderMutation = useMutation({
    mutationFn: async (count) => {
      const promises = Array(count).fill(null).map(() => createRandomOrder());
      return Promise.all(promises);
    },
    onSuccess: (data) => {
      toast.success(` Created ${data.length} orders!`);
    },
    onError: (error) => {
      toast.error(` Bulk creation failed: ${error.message}`);
    },
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    
    let orderId = formData.orderId;
    

    if (errorSimulation) {
      const lastTwo = orderId.slice(-2);
      orderId = orderId.slice(0, -2) + errorSimulation;
    }

    createOrderMutation.mutate({
      orderId,
      product: formData.product,
      price: parseFloat(formData.price),
    });
  };

  const handleInputChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const errorSimulations = [
    { value: '', label: 'None (Success)', description: 'Order will process normally' },
    { value: '99', label: 'Network Error', description: 'Temporary failure - will retry 3 times' },
    { value: '98', label: 'Database Timeout', description: 'Temporary failure - will retry' },
    { value: '97', label: 'Service Unavailable', description: 'Temporary failure - will retry' },
    { value: '96', label: 'Rate Limit', description: 'Temporary failure - will retry' },
    { value: '88', label: 'Validation Error', description: 'Permanent failure - goes to DLQ' },
    { value: '77', label: 'Duplicate Order', description: 'Permanent failure - goes to DLQ' },
    { value: '66', label: 'Product Not Found', description: 'Permanent failure - goes to DLQ' },
    { value: '55', label: 'Insufficient Inventory', description: 'Permanent failure - goes to DLQ' },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Order Producer</h1>
        <p className="text-gray-600 mt-1">Create and submit orders to Kafka</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Manual Order Form */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <PlusCircle className="w-5 h-5 mr-2" />
              Create Custom Order
            </CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Order ID
                </label>
                <input
                  type="text"
                  name="orderId"
                  value={formData.orderId}
                  onChange={handleInputChange}
                  placeholder="e.g., ORDER-1234"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Product Name
                </label>
                <input
                  type="text"
                  name="product"
                  value={formData.product}
                  onChange={handleInputChange}
                  placeholder="e.g., Laptop, Phone, Monitor"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Price ($)
                </label>
                <input
                  type="number"
                  name="price"
                  value={formData.price}
                  onChange={handleInputChange}
                  placeholder="e.g., 999.99"
                  step="0.01"
                  min="0"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Error Simulation
                </label>
                <select
                  value={errorSimulation}
                  onChange={(e) => setErrorSimulation(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {errorSimulations.map((sim) => (
                    <option key={sim.value} value={sim.value}>
                      {sim.label}
                    </option>
                  ))}
                </select>
                <p className="mt-1 text-xs text-gray-500">
                  {errorSimulations.find(s => s.value === errorSimulation)?.description}
                </p>
              </div>

              <button
                type="submit"
                disabled={createOrderMutation.isPending}
                className="w-full bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium"
              >
                {createOrderMutation.isPending ? 'Creating...' : 'Create Order'}
              </button>
            </form>
          </CardContent>
        </Card>

        {/* Quick Actions */}
        <div className="space-y-6">
          {/* Random Order */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Shuffle className="w-5 h-5 mr-2" />
                Random Order Generator
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <p className="text-sm text-gray-600">
                Generate random orders for testing. Product names and prices are randomized.
              </p>
              <button
                onClick={() => randomOrderMutation.mutate()}
                disabled={randomOrderMutation.isPending}
                className="w-full bg-green-600 text-white py-2 px-4 rounded-lg hover:bg-green-700 disabled:opacity-50 transition-colors font-medium"
              >
                {randomOrderMutation.isPending ? 'Generating...' : 'Generate Random Order'}
              </button>
            </CardContent>
          </Card>

          {/* Bulk Orders */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Zap className="w-5 h-5 mr-2" />
                Bulk Order Creation
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <p className="text-sm text-gray-600">
                Create multiple random orders at once for load testing.
              </p>
              <div className="grid grid-cols-3 gap-3">
                <button
                  onClick={() => bulkOrderMutation.mutate(10)}
                  disabled={bulkOrderMutation.isPending}
                  className="bg-purple-600 text-white py-2 px-4 rounded-lg hover:bg-purple-700 disabled:opacity-50 transition-colors text-sm font-medium"
                >
                  10 Orders
                </button>
                <button
                  onClick={() => bulkOrderMutation.mutate(50)}
                  disabled={bulkOrderMutation.isPending}
                  className="bg-purple-600 text-white py-2 px-4 rounded-lg hover:bg-purple-700 disabled:opacity-50 transition-colors text-sm font-medium"
                >
                  50 Orders
                </button>
                <button
                  onClick={() => bulkOrderMutation.mutate(100)}
                  disabled={bulkOrderMutation.isPending}
                  className="bg-purple-600 text-white py-2 px-4 rounded-lg hover:bg-purple-700 disabled:opacity-50 transition-colors text-sm font-medium"
                >
                  100 Orders
                </button>
              </div>
              {bulkOrderMutation.isPending && (
                <div className="text-center">
                  <div className="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-purple-600"></div>
                  <p className="text-sm text-gray-600 mt-2">Creating orders...</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Error Simulation Guide */}
          <Card className="bg-blue-50 border-blue-200">
            <CardHeader>
              <CardTitle className="text-blue-900"> Testing Guide</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2 text-sm text-blue-800">
                <p className="font-medium">Order ID patterns for error simulation:</p>
                <ul className="list-disc list-inside space-y-1 ml-2">
                  <li><code className="bg-blue-100 px-1 rounded">*99</code> - Network error (retries)</li>
                  <li><code className="bg-blue-100 px-1 rounded">*88</code> - Validation error (DLQ)</li>
                  <li><code className="bg-blue-100 px-1 rounded">*77</code> - Duplicate (DLQ)</li>
                  <li>Other - Success </li>
                </ul>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default Producer;