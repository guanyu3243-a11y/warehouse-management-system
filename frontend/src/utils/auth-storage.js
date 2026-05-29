const TOKEN_KEY = 'wms_token'
const USER_KEY = 'wms_user'

export function getToken() {
  return window.localStorage.getItem(TOKEN_KEY)
}

export function getStoredUser() {
  const rawUser = window.localStorage.getItem(USER_KEY)

  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser)
  } catch {
    window.localStorage.removeItem(USER_KEY)
    return null
  }
}

export function setAuthStorage(token, user) {
  window.localStorage.setItem(TOKEN_KEY, token)
  window.localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuthStorage() {
  window.localStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(USER_KEY)
}
