import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { PurchasesPage } from './pages/PurchasesPage';
import { TransfersPage } from './pages/TransfersPage';
import { AssignmentsPage } from './pages/AssignmentsPage';
import { UserManagementPage } from './pages/UserManagementPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          {/* Public Route */}
          <Route path="/login" element={<LoginPage />} />

          {/* Protected Routes inside Layout */}
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/dashboard" replace />} />
            
            {/* Dashboard: All roles */}
            <Route
              path="dashboard"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER']}>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />

            {/* Purchases: All roles */}
            <Route
              path="purchases"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER']}>
                  <PurchasesPage />
                </ProtectedRoute>
              }
            />

            {/* Transfers: All roles */}
            <Route
              path="transfers"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'BASE_COMMANDER', 'LOGISTICS_OFFICER']}>
                  <TransfersPage />
                </ProtectedRoute>
              }
            />

            {/* Assignments & Expenditures: ADMIN & BASE_COMMANDER only */}
            <Route
              path="assignments"
              element={
                <ProtectedRoute allowedRoles={['ADMIN', 'BASE_COMMANDER']}>
                  <AssignmentsPage />
                </ProtectedRoute>
              }
            />

            {/* User Management: ADMIN only */}
            <Route
              path="users"
              element={
                <ProtectedRoute allowedRoles={['ADMIN']}>
                  <UserManagementPage />
                </ProtectedRoute>
              }
            />
          </Route>

          {/* Catch-all */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
