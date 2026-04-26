import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, renderHook } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

// Use vi.hoisted to properly hoist mock definitions
const { mockCheckAuth } = vi.hoisted(() => ({
  mockCheckAuth: vi.fn(),
}))

vi.mock('../services/api', () => ({
  spotifyApi: {
    checkAuth: mockCheckAuth,
  },
}))

import { AuthProvider, useAuth } from './AuthContext'

describe('AuthContext', () => {
  beforeEach(() => {
    mockCheckAuth.mockClear()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('AuthProvider', () => {
    it('should provide auth context to children', () => {
      mockCheckAuth.mockResolvedValue({
        data: null,
      })

      render(
        <AuthProvider>
          <div>Test Child</div>
        </AuthProvider>
      )

      expect(screen.getByText('Test Child')).toBeInTheDocument()
    })

    it('should call checkAuth on mount', async () => {
      mockCheckAuth.mockResolvedValue({
        data: null,
      })

      render(
        <AuthProvider>
          <div>Test</div>
        </AuthProvider>
      )

      await waitFor(() => {
        expect(mockCheckAuth).toHaveBeenCalledTimes(1)
      })
    })

    it('should set loading state initially', async () => {
      mockCheckAuth.mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve({ data: null }), 100))
      )

      const TestComponent = () => {
        const { isLoading } = useAuth()
        return <div>{isLoading ? 'Loading' : 'Ready'}</div>
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      expect(screen.getByText('Loading')).toBeInTheDocument()

      await waitFor(() => {
        expect(screen.getByText('Ready')).toBeInTheDocument()
      })
    })
  })

  describe('useAuth hook', () => {
    it('should throw error when used outside AuthProvider', () => {
      mockCheckAuth.mockResolvedValue({
        data: null,
      })

      // Mock console.error to suppress error output in test
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

      expect(() => {
        renderHook(() => useAuth())
      }).toThrow('useAuth must be used within an AuthProvider')

      consoleSpy.mockRestore()
    })

    it('should return auth context when used inside AuthProvider', async () => {
      mockCheckAuth.mockResolvedValue({
        data: null,
      })

      const { result } = renderHook(() => useAuth(), {
        wrapper: AuthProvider,
      })

      await waitFor(() => {
        expect(result.current).toBeDefined()
        expect(result.current).toHaveProperty('user')
        expect(result.current).toHaveProperty('isAuthenticated')
        expect(result.current).toHaveProperty('isLoading')
        expect(result.current).toHaveProperty('logout')
      })
    })
  })

  describe('authentication state', () => {
    it('should set user when authentication check returns data', async () => {
      const mockUser = {
        id: 'user123',
        email: 'user@example.com',
        displayName: 'Test User',
      }

      mockCheckAuth.mockResolvedValue({
        data: mockUser,
      })

      const TestComponent = () => {
        const { user, isAuthenticated } = useAuth()
        return (
          <div>
            {isAuthenticated && <div>{user?.displayName}</div>}
          </div>
        )
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('Test User')).toBeInTheDocument()
      })
    })

    it('should set isAuthenticated to true when user exists', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { id: 'user123' },
      })

      const TestComponent = () => {
        const { isAuthenticated } = useAuth()
        return <div>{isAuthenticated ? 'Authenticated' : 'Not Authenticated'}</div>
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('Authenticated')).toBeInTheDocument()
      })
    })

    it('should set isAuthenticated to false when user is null', async () => {
      mockCheckAuth.mockResolvedValue({
        data: null,
      })

      const TestComponent = () => {
        const { isAuthenticated } = useAuth()
        return <div>{isAuthenticated ? 'Authenticated' : 'Not Authenticated'}</div>
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('Not Authenticated')).toBeInTheDocument()
      })
    })

    it('should handle auth check errors gracefully', async () => {
      mockCheckAuth.mockRejectedValue(
        new Error('Network error')
      )

      const TestComponent = () => {
        const { isAuthenticated, isLoading } = useAuth()
        return (
          <div>
            {isLoading && <div>Loading</div>}
            {!isLoading && !isAuthenticated && <div>Not logged in</div>}
          </div>
        )
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('Not logged in')).toBeInTheDocument()
      })
    })
  })

  describe('logout function', () => {
    it('should clear user on logout', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { id: 'user123', displayName: 'Test User' },
      })

      const TestComponent = () => {
        const { user, isAuthenticated, logout } = useAuth()
        return (
          <div>
            {isAuthenticated && (
              <>
                <div>{user?.displayName}</div>
                <button onClick={logout}>Logout</button>
              </>
            )}
            {!isAuthenticated && <div>Not logged in</div>}
          </div>
        )
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('Test User')).toBeInTheDocument()
      })

      const logoutButton = screen.getByRole('button', { name: 'Logout' })
      await userEvent.click(logoutButton)

      await waitFor(() => {
        expect(screen.getByText('Not logged in')).toBeInTheDocument()
      })
    })

    it('should set isAuthenticated to false after logout', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { id: 'user123' },
      })

      const TestComponent = () => {
        const { isAuthenticated, logout } = useAuth()
        return (
          <div>
            <div>{isAuthenticated ? 'Authenticated' : 'Not Authenticated'}</div>
            <button onClick={logout}>Logout</button>
          </div>
        )
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('Authenticated')).toBeInTheDocument()
      })

      const logoutButton = screen.getByRole('button', { name: 'Logout' })
      await userEvent.click(logoutButton)

      await waitFor(() => {
        expect(screen.getByText('Not Authenticated')).toBeInTheDocument()
      })
    })
  })

  describe('loading state', () => {
    it('should set isLoading to false after auth check completes', async () => {
      mockCheckAuth.mockResolvedValue({
        data: null,
      })

      const TestComponent = () => {
        const { isLoading } = useAuth()
        return <div>{isLoading ? 'Loading' : 'Done'}</div>
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('Done')).toBeInTheDocument()
      })
    })

    it('should set isLoading to false even if auth check fails', async () => {
      mockCheckAuth.mockRejectedValue(
        new Error('Network error')
      )

      const TestComponent = () => {
        const { isLoading } = useAuth()
        return <div>{isLoading ? 'Loading' : 'Done'}</div>
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('Done')).toBeInTheDocument()
      })
    })
  })

  describe('multiple components using useAuth', () => {
    it('should share auth state across multiple components', async () => {
      mockCheckAuth.mockResolvedValue({
        data: { id: 'user123', displayName: 'Shared User' },
      })

      const Component1 = () => {
        const { user } = useAuth()
        return <div>Component 1: {user?.displayName}</div>
      }

      const Component2 = () => {
        const { isAuthenticated } = useAuth()
        return <div>Component 2: {isAuthenticated ? 'Logged in' : 'Logged out'}</div>
      }

      render(
        <AuthProvider>
          <Component1 />
          <Component2 />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('Component 1: Shared User')).toBeInTheDocument()
        expect(screen.getByText('Component 2: Logged in')).toBeInTheDocument()
      })
    })
  })
})
