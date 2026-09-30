import React, { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  LayoutDashboard,
  ShoppingCart,
  ArrowLeftRight,
  ClipboardList,
  Users,
  LogOut,
  Shield,
  Menu,
  X,
  Radio,
  Building2,
  User as UserIcon,
} from 'lucide-react';

export const Layout = () => {
  const { user, logout, role, baseName } = useAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = [
    {
      name: 'Dashboard',
      path: '/dashboard',
      icon: LayoutDashboard,
      roles: ['ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER'],
    },
    {
      name: 'Purchases',
      path: '/purchases',
      icon: ShoppingCart,
      roles: ['ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER'],
    },
    {
      name: 'Transfers',
      path: '/transfers',
      icon: ArrowLeftRight,
      roles: ['ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER'],
    },
    {
      name: 'Assignments & Expenditures',
      path: '/assignments',
      icon: ClipboardList,
      roles: ['ADMIN', 'BASE_COMMANDER'], // LOGISTICS_OFFICER denied
    },
    {
      name: 'User Management',
      path: '/users',
      icon: Users,
      roles: ['ADMIN'], // ADMIN only
    },
  ];

  const filteredNavItems = navItems.filter((item) => item.roles.includes(role));

  const getRoleBadgeColor = (r) => {
    switch (r) {
      case 'ADMIN':
        return 'bg-purple-900/60 text-purple-300 border-purple-500/40';
      case 'BASE_COMMANDER':
        return 'bg-amber-900/60 text-amber-300 border-amber-500/40';
      case 'LOGISTICS_OFFICER':
        return 'bg-cyan-900/60 text-cyan-300 border-cyan-500/40';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  return (
    <div className="flex h-screen bg-[#070b14] text-slate-200">
      {/* Sidebar for Desktop */}
      <aside className="hidden md:flex flex-col w-64 bg-[#0c1322] border-r border-slate-800/80">
        {/* Brand / Title */}
        <div className="flex items-center space-x-3 px-6 py-5 border-b border-slate-800/80">
          <div className="p-2 rounded bg-emerald-950/60 border border-emerald-500/30 text-emerald-400">
            <Shield className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-mono tracking-widest text-emerald-400 font-semibold uppercase">DEFENSE ASSET</div>
            <div className="text-base font-bold tracking-tight text-white">MAMS Command</div>
          </div>
        </div>

        {/* Live Status indicator */}
        <div className="mx-4 my-3 px-3 py-2 rounded bg-slate-900/90 border border-slate-800/60 flex items-center justify-between text-xs font-mono">
          <span className="flex items-center text-emerald-400">
            <Radio className="w-3.5 h-3.5 mr-1.5 animate-pulse" /> SYSTEM READY
          </span>
          <span className="text-slate-400">SECURE V1.0</span>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 px-3 space-y-1.5 mt-2 overflow-y-auto">
          {filteredNavItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center px-3.5 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 shadow-sm'
                      : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/50'
                  }`
                }
              >
                <Icon className="w-4 h-4 mr-3 shrink-0" />
                <span>{item.name}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* User Profile Card & Sign Out */}
        <div className="p-4 border-t border-slate-800/80 bg-slate-950/40">
          <div className="flex items-start space-x-3 mb-3">
            <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300 font-bold text-xs shrink-0">
              <UserIcon className="w-4 h-4" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="text-sm font-medium text-slate-100 truncate">{user?.fullName || 'User'}</div>
              <div className="text-xs text-slate-400 truncate">@{user?.username}</div>
              <div className="mt-1 flex flex-wrap gap-1">
                <span className={`inline-block px-1.5 py-0.5 rounded text-[10px] font-mono border ${getRoleBadgeColor(role)}`}>
                  {role}
                </span>
                {baseName ? (
                  <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-mono bg-slate-800/80 text-slate-300 border border-slate-700">
                    <Building2 className="w-2.5 h-2.5 mr-1" /> {baseName}
                  </span>
                ) : (
                  <span className="inline-block px-1.5 py-0.5 rounded text-[10px] font-mono bg-slate-800/50 text-slate-400 border border-slate-700/50">
                    HQ (Global)
                  </span>
                )}
              </div>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center space-x-2 px-3 py-2 rounded-lg text-xs font-mono uppercase tracking-wider text-rose-300 bg-rose-950/20 hover:bg-rose-900/30 border border-rose-900/40 transition-colors"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Mobile Header */}
        <header className="md:hidden flex items-center justify-between px-4 py-3 bg-[#0c1322] border-b border-slate-800">
          <div className="flex items-center space-x-2">
            <Shield className="w-5 h-5 text-emerald-400" />
            <span className="font-bold text-sm tracking-tight text-white">MAMS Command</span>
          </div>
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
          >
            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </header>

        {/* Mobile Drawer */}
        {mobileMenuOpen && (
          <div className="md:hidden bg-[#0c1322] border-b border-slate-800 px-4 py-3 space-y-2">
            {filteredNavItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={() => setMobileMenuOpen(false)}
                className={({ isActive }) =>
                  `flex items-center px-3 py-2 rounded text-sm ${
                    isActive ? 'bg-emerald-500/20 text-emerald-400' : 'text-slate-300'
                  }`
                }
              >
                <item.icon className="w-4 h-4 mr-2" />
                {item.name}
              </NavLink>
            ))}
            <div className="pt-2 border-t border-slate-800">
              <button
                onClick={handleLogout}
                className="w-full text-left flex items-center px-3 py-2 text-rose-400 text-sm"
              >
                <LogOut className="w-4 h-4 mr-2" /> Sign Out
              </button>
            </div>
          </div>
        )}

        {/* Dynamic Page Outlet */}
        <main className="flex-1 overflow-y-auto p-4 md:p-8 bg-[#070b14]">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
