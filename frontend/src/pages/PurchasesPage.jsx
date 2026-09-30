import React, { useState, useEffect, useMemo } from 'react';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import {
  ShoppingCart,
  Plus,
  Filter,
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
  Lock,
  Building2,
  Package,
  Calendar,
  DollarSign,
  User as UserIcon,
  X,
  FileSpreadsheet
} from 'lucide-react';

export const PurchasesPage = () => {
  const { user, role, baseId, baseName } = useAuth();

  const todayStr = useMemo(() => new Date().toISOString().split('T')[0], []);

  // Lookups
  const [bases, setBases] = useState([]);
  const [equipmentTypes, setEquipmentTypes] = useState([]);
  const [lookupsLoading, setLookupsLoading] = useState(true);

  // Table Data & State
  const [purchases, setPurchases] = useState([]);
  const [tableLoading, setTableLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);

  // Pagination State
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filter Bar State
  const [filterStartDate, setFilterStartDate] = useState('');
  const [filterEndDate, setFilterEndDate] = useState('');
  const [filterBaseId, setFilterBaseId] = useState(role === 'ADMIN' ? '' : (baseId ? String(baseId) : ''));
  const [filterEquipmentTypeId, setFilterEquipmentTypeId] = useState('');

  // Form State
  const [showFormModal, setShowFormModal] = useState(false);
  const [formBaseId, setFormBaseId] = useState(role === 'ADMIN' ? '' : (baseId ? String(baseId) : ''));
  const [formEquipmentTypeId, setFormEquipmentTypeId] = useState('');
  const [formQuantity, setFormQuantity] = useState('');
  const [formUnitCost, setFormUnitCost] = useState('');
  const [formVendor, setFormVendor] = useState('');
  const [formPurchaseDate, setFormPurchaseDate] = useState(todayStr);
  const [formNotes, setFormNotes] = useState('');

  // Form Submission & Validation State
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [formGeneralError, setFormGeneralError] = useState(null);
  const [toastMessage, setToastMessage] = useState(null);

  // Synchronize base ID if changed in AuthContext
  useEffect(() => {
    if (role !== 'ADMIN' && baseId) {
      setFormBaseId(String(baseId));
      setFilterBaseId(String(baseId));
    }
  }, [role, baseId]);

  // Load lookup metadata (bases & equipment types)
  useEffect(() => {
    const fetchLookups = async () => {
      setLookupsLoading(true);
      try {
        const [basesRes, equipRes] = await Promise.all([
          api.get('/api/bases'),
          api.get('/api/equipment-types'),
        ]);

        const basesData = Array.isArray(basesRes.data)
          ? basesRes.data
          : (basesRes.data?.data || []);
        const equipData = Array.isArray(equipRes.data)
          ? equipRes.data
          : (equipRes.data?.data || []);

        setBases(basesData);
        setEquipmentTypes(equipData);
      } catch (err) {
        console.error('Failed to load lookup data:', err);
      } finally {
        setLookupsLoading(false);
      }
    };

    fetchLookups();
  }, []);

  // Fetch paginated purchases
  const fetchPurchases = async () => {
    setTableLoading(true);
    setFetchError(null);

    try {
      const params = {
        page,
        size: pageSize,
        sort: 'purchaseDate,desc',
      };

      if (filterStartDate) params.start_date = filterStartDate;
      if (filterEndDate) params.end_date = filterEndDate;

      if (role === 'ADMIN') {
        if (filterBaseId) params.base_id = filterBaseId;
      } else if (baseId) {
        params.base_id = baseId;
      }

      if (filterEquipmentTypeId) params.equipment_type_id = filterEquipmentTypeId;

      const res = await api.get('/api/purchases', { params });
      const payload = res.data?.data || res.data;

      setPurchases(payload.content || []);
      setTotalPages(payload.totalPages || 0);
      setTotalElements(payload.totalElements || 0);
    } catch (err) {
      console.error('Failed to fetch purchases:', err);
      setFetchError(
        err.response?.data?.error || 'Unable to retrieve purchase history. Please verify your connection or try again.'
      );
    } finally {
      setTableLoading(false);
    }
  };

  useEffect(() => {
    fetchPurchases();
  }, [page, filterStartDate, filterEndDate, filterBaseId, filterEquipmentTypeId]);

  // Reset form inputs
  const resetForm = () => {
    setFormBaseId(role === 'ADMIN' ? '' : (baseId ? String(baseId) : ''));
    setFormEquipmentTypeId('');
    setFormQuantity('');
    setFormUnitCost('');
    setFormVendor('');
    setFormPurchaseDate(todayStr);
    setFormNotes('');
    setFieldErrors({});
    setFormGeneralError(null);
  };

  // Handle purchase submission
  const handleSubmitPurchase = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setFieldErrors({});
    setFormGeneralError(null);

    // Client-side quick validation
    const errors = {};
    const effectiveBase = role === 'ADMIN' ? formBaseId : (baseId || formBaseId);
    if (!effectiveBase) {
      errors.baseId = 'Please select a military base';
    }
    if (!formEquipmentTypeId) {
      errors.equipmentTypeId = 'Please select equipment type';
    }
    const qtyNum = parseInt(formQuantity, 10);
    if (!qtyNum || qtyNum <= 0) {
      errors.quantity = 'Quantity must be at least 1';
    }
    if (!formPurchaseDate) {
      errors.purchaseDate = 'Purchase date is required';
    } else if (formPurchaseDate > todayStr) {
      errors.purchaseDate = 'Purchase date cannot be in the future';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      setSubmitting(false);
      return;
    }

    const payload = {
      base_id: Number(effectiveBase),
      equipment_type_id: Number(formEquipmentTypeId),
      quantity: qtyNum,
      unit_cost: formUnitCost !== '' ? parseFloat(formUnitCost) : null,
      vendor: formVendor.trim() || null,
      purchase_date: formPurchaseDate,
      notes: formNotes.trim() || null,
    };

    try {
      const res = await api.post('/api/purchases', payload);
      const createdItem = res.data?.data || res.data;

      // Toast notification
      setToastMessage({
        type: 'success',
        text: `Purchase order #PCH-${createdItem.id} logged successfully for ${createdItem.quantity}x ${createdItem.equipmentName || 'units'}.`,
      });

      // Clear & close form
      resetForm();
      setShowFormModal(false);

      // Auto-hide toast after 5s
      setTimeout(() => setToastMessage(null), 5000);

      // If not on first page, go to first page, else re-fetch
      if (page !== 0) {
        setPage(0);
      } else {
        fetchPurchases();
      }
    } catch (err) {
      console.error('Error recording purchase:', err);
      const errData = err.response?.data;

      if (errData?.details && typeof errData.details === 'object') {
        // Map backend field errors
        const mappedErrors = {};
        for (const [key, msg] of Object.entries(errData.details)) {
          if (key === 'baseId' || key === 'base_id') mappedErrors.baseId = msg;
          else if (key === 'equipmentTypeId' || key === 'equipment_type_id') mappedErrors.equipmentTypeId = msg;
          else if (key === 'quantity') mappedErrors.quantity = msg;
          else if (key === 'purchaseDate' || key === 'purchase_date') mappedErrors.purchaseDate = msg;
          else if (key === 'unitCost' || key === 'unit_cost') mappedErrors.unitCost = msg;
          else mappedErrors[key] = msg;
        }
        setFieldErrors(mappedErrors);
      }

      setFormGeneralError(
        errData?.error || 'Failed to record purchase. Please verify the input values.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  const clearFilters = () => {
    setFilterStartDate('');
    setFilterEndDate('');
    if (role === 'ADMIN') setFilterBaseId('');
    setFilterEquipmentTypeId('');
    setPage(0);
  };

  return (
    <div className="space-y-6">
      {/* Top Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800/80">
        <div>
          <div className="flex items-center space-x-2">
            <span className="p-1.5 rounded-lg bg-emerald-950/60 border border-emerald-500/30 text-emerald-400">
              <ShoppingCart className="w-5 h-5" />
            </span>
            <h1 className="text-xl md:text-2xl font-bold tracking-tight text-white">
              Procurement & Purchases
            </h1>
          </div>
          <p className="text-xs font-mono text-slate-400 mt-1">
            LOGISTICS AUDIT LEDGER • IMMUTABLE ASSET ACQUISITION RECORDS
          </p>
        </div>

        {/* Action Button */}
        <div className="flex items-center space-x-3">
          <button
            id="open-record-purchase-modal"
            onClick={() => {
              setFieldErrors({});
              setFormGeneralError(null);
              setShowFormModal(true);
            }}
            className="inline-flex items-center space-x-2 px-4 py-2.5 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs uppercase font-mono tracking-wider transition-all shadow-lg shadow-emerald-500/20 active:scale-95"
          >
            <Plus className="w-4 h-4 stroke-[3]" />
            <span>Record Purchase</span>
          </button>
        </div>
      </div>

      {/* Global Toast Banner */}
      {toastMessage && (
        <div
          className={`p-3.5 rounded-xl border flex items-center justify-between text-xs font-mono transition-all animate-fade-in ${
            toastMessage.type === 'success'
              ? 'bg-emerald-950/50 border-emerald-500/40 text-emerald-200'
              : 'bg-rose-950/50 border-rose-500/40 text-rose-200'
          }`}
        >
          <div className="flex items-center space-x-2.5">
            {toastMessage.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            )}
            <span>{toastMessage.text}</span>
          </div>
          <button
            onClick={() => setToastMessage(null)}
            className="text-slate-400 hover:text-white p-1"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Filter Bar */}
      <div className="p-4 rounded-xl bg-[#0c1322] border border-slate-800 shadow-md">
        <div className="flex items-center justify-between mb-3 text-xs font-mono text-slate-400">
          <span className="flex items-center">
            <Filter className="w-3.5 h-3.5 mr-1.5 text-emerald-400" />
            FILTER HISTORY
          </span>
          {(filterStartDate || filterEndDate || (role === 'ADMIN' && filterBaseId) || filterEquipmentTypeId) && (
            <button
              onClick={clearFilters}
              className="text-[11px] text-slate-400 hover:text-emerald-400 underline decoration-dotted"
            >
              Reset Filters
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Start Date */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-wider text-slate-400 mb-1">
              Start Date
            </label>
            <input
              type="date"
              id="filter-start-date"
              value={filterStartDate}
              max={todayStr}
              onChange={(e) => {
                setFilterStartDate(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-emerald-500/60 focus:outline-none"
            />
          </div>

          {/* End Date */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-wider text-slate-400 mb-1">
              End Date
            </label>
            <input
              type="date"
              id="filter-end-date"
              value={filterEndDate}
              max={todayStr}
              onChange={(e) => {
                setFilterEndDate(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-emerald-500/60 focus:outline-none"
            />
          </div>

          {/* Base Filter (Only enabled/shown as selectable for ADMIN) */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-wider text-slate-400 mb-1">
              Military Base
            </label>
            {role === 'ADMIN' ? (
              <select
                id="filter-base-select"
                value={filterBaseId}
                onChange={(e) => {
                  setFilterBaseId(e.target.value);
                  setPage(0);
                }}
                className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-emerald-500/60 focus:outline-none"
              >
                <option value="">All Bases</option>
                {bases.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                  </option>
                ))}
              </select>
            ) : (
              <div
                title="Your access is scoped strictly to your assigned base"
                className="w-full px-3 py-2 rounded-lg bg-slate-900/80 border border-slate-800 text-xs text-slate-400 font-mono flex items-center justify-between"
              >
                <span className="truncate">{baseName || 'Assigned Base'}</span>
                <Lock className="w-3.5 h-3.5 text-amber-500/70 ml-1.5 shrink-0" />
              </div>
            )}
          </div>

          {/* Equipment Type Filter */}
          <div>
            <label className="block text-[10px] font-mono uppercase tracking-wider text-slate-400 mb-1">
              Equipment Type
            </label>
            <select
              id="filter-equipment-select"
              value={filterEquipmentTypeId}
              onChange={(e) => {
                setFilterEquipmentTypeId(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-xs text-slate-200 font-mono focus:border-emerald-500/60 focus:outline-none"
            >
              <option value="">All Equipment</option>
              {equipmentTypes.map((eq) => (
                <option key={eq.id} value={eq.id}>
                  {eq.name} ({eq.category})
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* History Ledger Section */}
      <div className="rounded-xl border border-slate-800 overflow-hidden bg-[#0c1322] shadow-xl">
        <div className="p-4 border-b border-slate-800/80 flex items-center justify-between bg-slate-950/40">
          <div className="flex items-center space-x-2">
            <FileSpreadsheet className="w-4 h-4 text-emerald-400" />
            <span className="text-xs font-mono font-semibold uppercase tracking-wider text-slate-200">
              Purchases Ledger
            </span>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-mono bg-slate-800 text-slate-300">
              {totalElements} entries
            </span>
          </div>

          <button
            onClick={fetchPurchases}
            disabled={tableLoading}
            title="Refresh Ledger"
            className="p-1.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${tableLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>

        {/* Error State */}
        {fetchError && (
          <div className="p-8 text-center bg-rose-950/20 border-b border-rose-900/30">
            <AlertCircle className="w-8 h-8 text-rose-500 mx-auto mb-2" />
            <p className="text-sm font-semibold text-rose-300 mb-1">{fetchError}</p>
            <p className="text-xs text-slate-400 font-mono mb-4">
              Unable to complete query on /api/purchases.
            </p>
            <button
              onClick={fetchPurchases}
              className="px-4 py-1.5 rounded bg-rose-900/60 hover:bg-rose-900 text-rose-200 text-xs font-mono transition-colors"
            >
              Retry Query
            </button>
          </div>
        )}

        {/* Loading Skeleton */}
        {tableLoading && (
          <div className="p-6 space-y-3">
            {[1, 2, 3, 4, 5].map((i) => (
              <div
                key={i}
                className="h-10 rounded-lg bg-slate-900/80 animate-pulse border border-slate-800/50"
              />
            ))}
          </div>
        )}

        {/* Empty State */}
        {!tableLoading && !fetchError && purchases.length === 0 && (
          <div className="p-12 text-center">
            <Package className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <h3 className="text-sm font-semibold text-slate-300">No purchases recorded yet</h3>
            <p className="text-xs text-slate-500 font-mono mt-1 max-w-sm mx-auto">
              There are no procurement entries matching the current filter parameters in the audit ledger.
            </p>
            <button
              onClick={() => setShowFormModal(true)}
              className="mt-4 inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-emerald-950/60 border border-emerald-500/40 text-emerald-400 hover:bg-emerald-900/50 text-xs font-mono transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Record First Purchase</span>
            </button>
          </div>
        )}

        {/* Desktop / Tablet Table View (hidden on small mobile screens) */}
        {!tableLoading && !fetchError && purchases.length > 0 && (
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead className="bg-slate-900 font-mono uppercase text-[11px] text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="p-3.5">Date</th>
                  <th className="p-3.5">Base</th>
                  <th className="p-3.5">Equipment</th>
                  <th className="p-3.5 text-right">Quantity</th>
                  <th className="p-3.5 text-right">Unit Cost</th>
                  <th className="p-3.5 text-right">Total Cost</th>
                  <th className="p-3.5">Vendor</th>
                  <th className="p-3.5">Recorded By</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono text-slate-300">
                {purchases.map((p) => {
                  const unitCostFormatted = p.unitCost != null ? Number(p.unitCost).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00';
                  const totalCostFormatted = p.totalCost != null ? Number(p.totalCost).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00';

                  return (
                    <tr key={p.id} className="hover:bg-slate-800/40 transition-colors">
                      <td className="p-3.5 text-slate-200 whitespace-nowrap">
                        {p.purchaseDate || p.purchase_date}
                      </td>
                      <td className="p-3.5 font-medium text-slate-100 whitespace-nowrap">
                        {p.baseName || p.base_name || `Base #${p.baseId || p.base_id}`}
                      </td>
                      <td className="p-3.5 text-white font-medium">
                        {p.equipmentName || p.equipment_name || `Item #${p.equipmentTypeId || p.equipment_type_id}`}
                      </td>
                      <td className="p-3.5 text-right font-bold text-emerald-400">
                        +{Number(p.quantity).toLocaleString()}
                      </td>
                      <td className="p-3.5 text-right text-slate-300">
                        ${unitCostFormatted}
                      </td>
                      <td className="p-3.5 text-right font-semibold text-slate-100">
                        ${totalCostFormatted}
                      </td>
                      <td className="p-3.5 text-slate-300 max-w-[140px] truncate" title={p.vendor}>
                        {p.vendor || '—'}
                      </td>
                      <td className="p-3.5 text-slate-400 whitespace-nowrap">
                        <span className="inline-flex items-center text-[11px] bg-slate-900 border border-slate-800 px-2 py-0.5 rounded">
                          <UserIcon className="w-2.5 h-2.5 mr-1 text-slate-500" />
                          @{p.createdByUsername || p.created_by_username || 'system'}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Mobile Stacked Cards View (visible on small screens only) */}
        {!tableLoading && !fetchError && purchases.length > 0 && (
          <div className="md:hidden divide-y divide-slate-800/70">
            {purchases.map((p) => {
              const unitCostFormatted = p.unitCost != null ? Number(p.unitCost).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00';
              const totalCostFormatted = p.totalCost != null ? Number(p.totalCost).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00';

              return (
                <div key={p.id} className="p-4 space-y-2.5 font-mono text-xs">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-white">
                      {p.equipmentName || p.equipment_name || 'Equipment'}
                    </span>
                    <span className="px-2 py-0.5 rounded bg-emerald-950/60 border border-emerald-500/40 text-emerald-400 font-bold">
                      +{Number(p.quantity).toLocaleString()} units
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-slate-400 text-[11px]">
                    <div>
                      <span className="text-slate-500 block">Date</span>
                      <span className="text-slate-200">{p.purchaseDate || p.purchase_date}</span>
                    </div>
                    <div>
                      <span className="text-slate-500 block">Base</span>
                      <span className="text-slate-200">{p.baseName || p.base_name}</span>
                    </div>
                    <div>
                      <span className="text-slate-500 block">Unit Cost</span>
                      <span className="text-slate-200">${unitCostFormatted}</span>
                    </div>
                    <div>
                      <span className="text-slate-500 block">Total Cost</span>
                      <span className="text-slate-100 font-semibold">${totalCostFormatted}</span>
                    </div>
                    <div className="col-span-2">
                      <span className="text-slate-500 block">Vendor</span>
                      <span className="text-slate-300">{p.vendor || '—'}</span>
                    </div>
                  </div>

                  <div className="pt-1.5 border-t border-slate-900 flex items-center justify-between text-[11px] text-slate-400">
                    <span>Ref #PCH-{p.id}</span>
                    <span className="inline-flex items-center bg-slate-900 px-1.5 py-0.5 rounded border border-slate-800">
                      <UserIcon className="w-2.5 h-2.5 mr-1 text-slate-500" />
                      @{p.createdByUsername || p.created_by_username || 'system'}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Pagination Controls */}
        {!tableLoading && !fetchError && totalPages > 0 && (
          <div className="p-3.5 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs font-mono text-slate-400 bg-slate-950/30">
            <div>
              Showing page <span className="text-slate-200 font-semibold">{page + 1}</span> of{' '}
              <span className="text-slate-200 font-semibold">{Math.max(1, totalPages)}</span> ({totalElements} total records)
            </div>

            <div className="flex items-center space-x-2">
              <button
                id="pagination-prev"
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="inline-flex items-center px-2.5 py-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-300 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
              >
                <ChevronLeft className="w-3.5 h-3.5 mr-1" />
                Previous
              </button>

              <button
                id="pagination-next"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="inline-flex items-center px-2.5 py-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-300 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
              >
                Next
                <ChevronRight className="w-3.5 h-3.5 ml-1" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Record Purchase Modal */}
      {showFormModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="w-full max-w-lg bg-[#0c1322] border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto">
            {/* Modal Title */}
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
              <div className="flex items-center space-x-2.5">
                <div className="p-2 rounded-lg bg-emerald-950/60 border border-emerald-500/30 text-emerald-400">
                  <ShoppingCart className="w-4 h-4" />
                </div>
                <div>
                  <h2 className="text-base font-bold text-white tracking-tight">Record Asset Purchase</h2>
                  <p className="text-[11px] font-mono text-slate-400">
                    MAMS IMMUTABLE ASSET PROCUREMENT ENTRY
                  </p>
                </div>
              </div>
              <button
                onClick={() => {
                  setShowFormModal(false);
                  resetForm();
                }}
                className="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* General Form Error Alert */}
            {formGeneralError && (
              <div className="p-3 rounded-lg bg-rose-950/40 border border-rose-500/40 text-rose-300 text-xs font-mono flex items-start space-x-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                <span>{formGeneralError}</span>
              </div>
            )}

            {/* Purchase Form */}
            <form onSubmit={handleSubmitPurchase} className="space-y-3.5 font-mono text-xs">
              {/* Base Selection */}
              <div>
                <label className="block text-slate-400 mb-1">
                  Destination Military Base <span className="text-rose-400">*</span>
                </label>
                {role === 'ADMIN' ? (
                  <select
                    id="form-base-select"
                    required
                    value={formBaseId}
                    onChange={(e) => {
                      setFormBaseId(e.target.value);
                      if (fieldErrors.baseId) setFieldErrors((prev) => ({ ...prev, baseId: null }));
                    }}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                      fieldErrors.baseId ? 'border-rose-500' : 'border-slate-800'
                    } text-slate-200 focus:border-emerald-500/60 focus:outline-none`}
                  >
                    <option value="">Select Base</option>
                    {bases.map((b) => (
                      <option key={b.id} value={b.id}>
                        {b.name} ({b.location})
                      </option>
                    ))}
                  </select>
                ) : (
                  <div
                    title="Locked to your assigned command jurisdiction"
                    className="w-full px-3 py-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-300 flex items-center justify-between"
                  >
                    <div className="flex items-center space-x-2">
                      <Building2 className="w-4 h-4 text-slate-500" />
                      <span>{baseName || `Base #${baseId}`}</span>
                    </div>
                    <span className="inline-flex items-center text-[10px] text-amber-400 bg-amber-950/40 px-2 py-0.5 rounded border border-amber-500/30">
                      <Lock className="w-3 h-3 mr-1" /> LOCKED TO JURISDICTION
                    </span>
                  </div>
                )}
                {fieldErrors.baseId && (
                  <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.baseId}</p>
                )}
              </div>

              {/* Equipment Type Selection */}
              <div>
                <label className="block text-slate-400 mb-1">
                  Equipment / Asset Type <span className="text-rose-400">*</span>
                </label>
                <select
                  id="form-equipment-select"
                  required
                  value={formEquipmentTypeId}
                  onChange={(e) => {
                    setFormEquipmentTypeId(e.target.value);
                    if (fieldErrors.equipmentTypeId) setFieldErrors((prev) => ({ ...prev, equipmentTypeId: null }));
                  }}
                  className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                    fieldErrors.equipmentTypeId ? 'border-rose-500' : 'border-slate-800'
                  } text-slate-200 focus:border-emerald-500/60 focus:outline-none`}
                >
                  <option value="">Select Equipment</option>
                  {equipmentTypes.map((eq) => (
                    <option key={eq.id} value={eq.id}>
                      {eq.name} • {eq.category?.toUpperCase()} ({eq.unit})
                    </option>
                  ))}
                </select>
                {fieldErrors.equipmentTypeId && (
                  <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.equipmentTypeId}</p>
                )}
              </div>

              {/* Quantity and Unit Cost */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1">
                    Quantity <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="number"
                    id="form-quantity-input"
                    min="1"
                    step="1"
                    required
                    placeholder="e.g. 50"
                    value={formQuantity}
                    onChange={(e) => {
                      setFormQuantity(e.target.value);
                      if (fieldErrors.quantity) setFieldErrors((prev) => ({ ...prev, quantity: null }));
                    }}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                      fieldErrors.quantity ? 'border-rose-500' : 'border-slate-800'
                    } text-slate-200 focus:border-emerald-500/60 focus:outline-none`}
                  />
                  {fieldErrors.quantity && (
                    <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.quantity}</p>
                  )}
                </div>

                <div>
                  <label className="block text-slate-400 mb-1">
                    Unit Cost ($) <span className="text-slate-500 text-[10px]">(optional)</span>
                  </label>
                  <input
                    type="number"
                    id="form-unitcost-input"
                    min="0"
                    step="0.01"
                    placeholder="0.00"
                    value={formUnitCost}
                    onChange={(e) => {
                      setFormUnitCost(e.target.value);
                      if (fieldErrors.unitCost) setFieldErrors((prev) => ({ ...prev, unitCost: null }));
                    }}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                      fieldErrors.unitCost ? 'border-rose-500' : 'border-slate-800'
                    } text-slate-200 focus:border-emerald-500/60 focus:outline-none`}
                  />
                  {fieldErrors.unitCost && (
                    <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.unitCost}</p>
                  )}
                </div>
              </div>

              {/* Vendor & Purchase Date */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1">
                    Vendor <span className="text-slate-500 text-[10px]">(optional)</span>
                  </label>
                  <input
                    type="text"
                    id="form-vendor-input"
                    placeholder="e.g. Lockheed Martin, Colt Defense"
                    value={formVendor}
                    onChange={(e) => setFormVendor(e.target.value)}
                    className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-slate-200 focus:border-emerald-500/60 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-slate-400 mb-1">
                    Purchase Date <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="date"
                    id="form-purchasedate-input"
                    max={todayStr}
                    required
                    value={formPurchaseDate}
                    onChange={(e) => {
                      setFormPurchaseDate(e.target.value);
                      if (fieldErrors.purchaseDate) setFieldErrors((prev) => ({ ...prev, purchaseDate: null }));
                    }}
                    className={`w-full px-3 py-2 rounded-lg bg-slate-950 border ${
                      fieldErrors.purchaseDate ? 'border-rose-500' : 'border-slate-800'
                    } text-slate-200 focus:border-emerald-500/60 focus:outline-none`}
                  />
                  {fieldErrors.purchaseDate && (
                    <p className="text-rose-400 text-[11px] mt-1">{fieldErrors.purchaseDate}</p>
                  )}
                </div>
              </div>

              {/* Notes */}
              <div>
                <label className="block text-slate-400 mb-1">
                  Logistics Notes <span className="text-slate-500 text-[10px]">(optional)</span>
                </label>
                <textarea
                  id="form-notes-input"
                  rows="2"
                  placeholder="Consignment identifier, batch #, delivery dock, etc."
                  value={formNotes}
                  onChange={(e) => setFormNotes(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg bg-slate-950 border border-slate-800 text-slate-200 focus:border-emerald-500/60 focus:outline-none"
                />
              </div>

              {/* Action Buttons */}
              <div className="flex items-center justify-end space-x-2.5 pt-4 border-t border-slate-800/80">
                <button
                  type="button"
                  onClick={() => {
                    setShowFormModal(false);
                    resetForm();
                  }}
                  className="px-4 py-2 rounded-lg bg-slate-800/80 hover:bg-slate-800 text-slate-300 font-mono text-xs transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  id="submit-record-purchase-btn"
                  disabled={submitting}
                  className="inline-flex items-center space-x-1.5 px-4 py-2 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold font-mono text-xs uppercase tracking-wider transition-all disabled:opacity-50"
                >
                  {submitting && <RefreshCw className="w-3.5 h-3.5 animate-spin mr-1" />}
                  <span>{submitting ? 'Recording...' : 'Commit Purchase'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
export default PurchasesPage;
