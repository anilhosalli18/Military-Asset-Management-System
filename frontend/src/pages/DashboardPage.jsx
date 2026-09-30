import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import {
  TrendingUp,
  Package,
  Layers,
  ArrowUpRight,
  ArrowDownLeft,
  UserCheck,
  Flame,
  Filter,
  X,
  RefreshCw,
  Building,
} from 'lucide-react';

export const DashboardPage = () => {
  const { user, role, baseId, baseName } = useAuth();

  const [metrics, setMetrics] = useState({
    openingBalance: 0,
    closingBalance: 0,
    netMovement: 0,
    purchases: 0,
    transfersIn: 0,
    transfersOut: 0,
    assigned: 0,
    expended: 0,
  });

  const [bases, setBases] = useState([]);
  const [equipmentTypes, setEquipmentTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [netMovementModalOpen, setNetMovementModalOpen] = useState(false);
  const [netMovementDetail, setNetMovementDetail] = useState(null);
  const [loadingDetail, setLoadingDetail] = useState(false);

  // Default to past 30 days so initial load provides required date range
  const todayStr = new Date().toISOString().split('T')[0];
  const thirtyDaysAgo = new Date();
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
  const thirtyDaysAgoStr = thirtyDaysAgo.toISOString().split('T')[0];

  const [startDate, setStartDate] = useState(thirtyDaysAgoStr);
  const [endDate, setEndDate] = useState(todayStr);
  const [selectedBaseId, setSelectedBaseId] = useState(role === 'ADMIN' ? '' : baseId || '');
  const [selectedEquipmentTypeId, setSelectedEquipmentTypeId] = useState('');

  // Fetch dropdown options
  useEffect(() => {
    const fetchOptions = async () => {
      try {
        const [basesRes, equipRes] = await Promise.all([
          api.get('/api/bases'),
          api.get('/api/equipment-types'),
        ]);
        setBases(basesRes.data);
        setEquipmentTypes(equipRes.data);
      } catch (err) {
        console.error("Failed to load filter options", err);
      }
    };
    fetchOptions();
  }, []);

  // Fetch metrics
  const fetchMetrics = async () => {
    setLoading(true);
    try {
      const params = {};
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;
      if (role === 'ADMIN') {
        if (selectedBaseId) params.baseId = selectedBaseId;
      } else {
        if (baseId) params.baseId = baseId;
      }
      if (selectedEquipmentTypeId) params.equipmentTypeId = selectedEquipmentTypeId;

      const response = await api.get('/api/dashboard/metrics', { params });
      const payload = response.data?.data || response.data;
      setMetrics(payload);
    } catch (err) {
      console.error("Failed to fetch dashboard metrics", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMetrics();
  }, [startDate, endDate, selectedBaseId, selectedEquipmentTypeId]);

  const handleOpenNetMovementModal = async () => {
    setNetMovementModalOpen(true);
    setLoadingDetail(true);
    try {
      const params = {};
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;
      if (role === 'ADMIN') {
        if (selectedBaseId) params.baseId = selectedBaseId;
      } else {
        if (baseId) params.baseId = baseId;
      }
      if (selectedEquipmentTypeId) params.equipmentTypeId = selectedEquipmentTypeId;

      const res = await api.get('/api/dashboard/net-movement-detail', { params });
      const payload = res.data?.data || res.data;
      setNetMovementDetail(payload);
    } catch (err) {
      console.error("Failed to fetch net movement details", err);
    } finally {
      setLoadingDetail(false);
    }
  };

  const metricCards = [
    {
      title: 'Opening Balance',
      value: metrics.openingBalance,
      icon: Layers,
      color: 'text-slate-300',
      bg: 'bg-slate-900/60',
      border: 'border-slate-800',
      sub: 'At start of selected period',
    },
    {
      title: 'Net Movement',
      value: (metrics.netMovement > 0 ? `+${metrics.netMovement}` : metrics.netMovement),
      icon: TrendingUp,
      color: metrics.netMovement >= 0 ? 'text-emerald-400' : 'text-rose-400',
      bg: 'bg-emerald-950/20',
      border: 'border-emerald-500/30',
      interactive: true,
      onClick: handleOpenNetMovementModal,
      badge: 'Click for line items',
    },
    {
      title: 'Closing Balance',
      value: metrics.closingBalance,
      icon: Package,
      color: 'text-blue-400',
      bg: 'bg-blue-950/20',
      border: 'border-blue-500/30',
      sub: 'Opening + Net Movement',
    },
    {
      title: 'Personnel Assigned',
      value: metrics.assigned,
      icon: UserCheck,
      color: 'text-amber-400',
      bg: 'bg-amber-950/20',
      border: 'border-amber-500/30',
      sub: 'Currently in active custody',
    },
    {
      title: 'Expended',
      value: metrics.expended,
      icon: Flame,
      color: 'text-rose-400',
      bg: 'bg-rose-950/20',
      border: 'border-rose-500/30',
      sub: 'Consumed / fired / destroyed',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <h1 className="text-xl md:text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            Asset Inventory & Readiness Dashboard
          </h1>
          <p className="text-xs font-mono text-slate-400 mt-1">
            DERIVED INVENTORY TELEMETRY • BASE SCOPE:{' '}
            <span className="text-emerald-400 font-semibold">
              {role === 'ADMIN' ? (selectedBaseId ? bases.find(b => b.id === Number(selectedBaseId))?.name || 'Selected Base' : 'ALL BASES (HQ)') : baseName || 'Assigned Base'}
            </span>
          </p>
        </div>
        <button
          onClick={fetchMetrics}
          className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-700 text-xs font-mono text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Filter Bar */}
      <div className="p-4 rounded-xl bg-[#0c1322] border border-slate-800 space-y-3">
        <div className="flex items-center space-x-2 text-xs font-mono uppercase tracking-wider text-slate-400">
          <Filter className="w-3.5 h-3.5 text-emerald-400" />
          <span>Telemetry Filter Criteria</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Start Date */}
          <div>
            <label className="block text-[11px] font-mono uppercase text-slate-400 mb-1">Start Date</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full px-3 py-1.5 rounded-lg bg-slate-950 border border-slate-700 text-xs text-slate-200 focus:outline-none focus:border-emerald-500 font-mono"
            />
          </div>
          {/* End Date */}
          <div>
            <label className="block text-[11px] font-mono uppercase text-slate-400 mb-1">End Date</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-full px-3 py-1.5 rounded-lg bg-slate-950 border border-slate-700 text-xs text-slate-200 focus:outline-none focus:border-emerald-500 font-mono"
            />
          </div>
          {/* Base Scope (locked for non-admins) */}
          <div>
            <label className="block text-[11px] font-mono uppercase text-slate-400 mb-1">Base Installation</label>
            {role === 'ADMIN' ? (
              <select
                value={selectedBaseId}
                onChange={(e) => setSelectedBaseId(e.target.value)}
                className="w-full px-3 py-1.5 rounded-lg bg-slate-950 border border-slate-700 text-xs text-slate-200 focus:outline-none focus:border-emerald-500 font-mono"
              >
                <option value="">All Bases (Global Fleet)</option>
                {bases.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} ({b.location})
                  </option>
                ))}
              </select>
            ) : (
              <div className="w-full px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 text-xs text-slate-400 font-mono flex items-center justify-between">
                <span>{baseName || 'Assigned Base'}</span>
                <span className="text-[10px] text-amber-500 uppercase">Enforced</span>
              </div>
            )}
          </div>
          {/* Equipment Type */}
          <div>
            <label className="block text-[11px] font-mono uppercase text-slate-400 mb-1">Equipment Type</label>
            <select
              value={selectedEquipmentTypeId}
              onChange={(e) => setSelectedEquipmentTypeId(e.target.value)}
              className="w-full px-3 py-1.5 rounded-lg bg-slate-950 border border-slate-700 text-xs text-slate-200 focus:outline-none focus:border-emerald-500 font-mono"
            >
              <option value="">All Equipment Categories</option>
              {equipmentTypes.map((eq) => (
                <option key={eq.id} value={eq.id}>
                  {eq.name} [{eq.category}]
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        {metricCards.map((card, idx) => {
          const Icon = card.icon;
          return (
            <div
              key={idx}
              onClick={card.interactive ? card.onClick : undefined}
              className={`p-5 rounded-xl border ${card.border} ${card.bg} relative overflow-hidden transition-all ${
                card.interactive
                  ? 'cursor-pointer hover:border-emerald-400 hover:shadow-lg hover:shadow-emerald-950/40 hover:-translate-y-0.5'
                  : ''
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono uppercase tracking-wider text-slate-400">{card.title}</span>
                <div className={`p-2 rounded-lg bg-slate-950/60 border border-slate-800/80 ${card.color}`}>
                  <Icon className="w-4 h-4" />
                </div>
              </div>
              <div className="mt-3">
                <div className={`text-2xl font-bold font-mono tracking-tight ${card.color}`}>
                  {loading ? '...' : card.value}
                </div>
                {card.badge ? (
                  <span className="inline-block mt-2 px-1.5 py-0.5 rounded text-[10px] font-mono bg-emerald-950 text-emerald-300 border border-emerald-500/30">
                    {card.badge}
                  </span>
                ) : (
                  <p className="text-[11px] text-slate-400 mt-1">{card.sub}</p>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Net Movement Detail Modal */}
      {netMovementModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="w-full max-w-4xl bg-[#0c1322] border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4 max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center space-x-2">
                <TrendingUp className="w-5 h-5 text-emerald-400" />
                <h2 className="text-lg font-bold text-white">Net Movement Line-Item Breakdown</h2>
              </div>
              <button
                onClick={() => setNetMovementModalOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-6 pr-2">
              {loadingDetail ? (
                <div className="py-12 text-center text-slate-400 font-mono text-xs">
                  Querying transaction logs...
                </div>
              ) : (
                <>
                  {/* Purchases Section */}
                  <div>
                    <h3 className="text-xs font-mono uppercase tracking-wider text-emerald-400 flex items-center mb-2">
                      <ArrowUpRight className="w-4 h-4 mr-1" /> Direct Purchases (Inflow)
                    </h3>
                    <div className="rounded-lg border border-slate-800 overflow-hidden bg-slate-950/40">
                      <table className="w-full text-xs text-left">
                        <thead className="bg-slate-900/80 font-mono text-slate-400 uppercase">
                          <tr>
                            <th className="p-2.5">Date</th>
                            <th className="p-2.5">Equipment</th>
                            <th className="p-2.5">Qty</th>
                            <th className="p-2.5">Vendor</th>
                            <th className="p-2.5">Total Cost</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-800/60 font-mono text-slate-300">
                          {netMovementDetail?.purchases?.length ? (
                            netMovementDetail.purchases.map((p) => (
                              <tr key={p.id}>
                                <td className="p-2.5">{p.purchaseDate}</td>
                                <td className="p-2.5 text-white">{p.equipmentTypeName}</td>
                                <td className="p-2.5 text-emerald-400 font-bold">+{p.quantity}</td>
                                <td className="p-2.5">{p.vendor}</td>
                                <td className="p-2.5">${p.totalCost}</td>
                              </tr>
                            ))
                          ) : (
                            <tr>
                              <td colSpan="5" className="p-4 text-center text-slate-500">No purchases found for criteria</td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>

                  {/* Transfers In Section */}
                  <div>
                    <h3 className="text-xs font-mono uppercase tracking-wider text-cyan-400 flex items-center mb-2">
                      <ArrowDownLeft className="w-4 h-4 mr-1" /> Transfers In (Inflow)
                    </h3>
                    <div className="rounded-lg border border-slate-800 overflow-hidden bg-slate-950/40">
                      <table className="w-full text-xs text-left">
                        <thead className="bg-slate-900/80 font-mono text-slate-400 uppercase">
                          <tr>
                            <th className="p-2.5">Date</th>
                            <th className="p-2.5">Equipment</th>
                            <th className="p-2.5">Qty</th>
                            <th className="p-2.5">From Base</th>
                            <th className="p-2.5">Status</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-800/60 font-mono text-slate-300">
                          {netMovementDetail?.transfersIn?.length ? (
                            netMovementDetail.transfersIn.map((t) => (
                              <tr key={t.id}>
                                <td className="p-2.5">{t.transferDate}</td>
                                <td className="p-2.5 text-white">{t.equipmentTypeName}</td>
                                <td className="p-2.5 text-cyan-400 font-bold">+{t.quantity}</td>
                                <td className="p-2.5">{t.fromBaseName}</td>
                                <td className="p-2.5 uppercase text-[10px]">{t.status}</td>
                              </tr>
                            ))
                          ) : (
                            <tr>
                              <td colSpan="5" className="p-4 text-center text-slate-500">No incoming transfers found</td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>

                  {/* Transfers Out Section */}
                  <div>
                    <h3 className="text-xs font-mono uppercase tracking-wider text-rose-400 flex items-center mb-2">
                      <ArrowUpRight className="w-4 h-4 mr-1" /> Transfers Out (Outflow)
                    </h3>
                    <div className="rounded-lg border border-slate-800 overflow-hidden bg-slate-950/40">
                      <table className="w-full text-xs text-left">
                        <thead className="bg-slate-900/80 font-mono text-slate-400 uppercase">
                          <tr>
                            <th className="p-2.5">Date</th>
                            <th className="p-2.5">Equipment</th>
                            <th className="p-2.5">Qty</th>
                            <th className="p-2.5">To Base</th>
                            <th className="p-2.5">Status</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-800/60 font-mono text-slate-300">
                          {netMovementDetail?.transfersOut?.length ? (
                            netMovementDetail.transfersOut.map((t) => (
                              <tr key={t.id}>
                                <td className="p-2.5">{t.transferDate}</td>
                                <td className="p-2.5 text-white">{t.equipmentTypeName}</td>
                                <td className="p-2.5 text-rose-400 font-bold">-{t.quantity}</td>
                                <td className="p-2.5">{t.toBaseName}</td>
                                <td className="p-2.5 uppercase text-[10px]">{t.status}</td>
                              </tr>
                            ))
                          ) : (
                            <tr>
                              <td colSpan="5" className="p-4 text-center text-slate-500">No outgoing transfers found</td>
                            </tr>
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </>
              )}
            </div>

            <div className="pt-3 border-t border-slate-800 flex justify-end">
              <button
                onClick={() => setNetMovementModalOpen(false)}
                className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-mono text-slate-200"
              >
                Close Modal
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
