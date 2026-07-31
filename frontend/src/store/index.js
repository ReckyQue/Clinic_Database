import { createStore } from 'vuex';
import Cookies from 'js-cookie';

export default createStore({
  state: {
    user: null,
    token: Cookies.get('token') || '',
    userRole: Cookies.get('userRole') || 'guest',
    homeRefreshKey: 0,
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token;
      Cookies.set('token', token);
    },
    SET_USER(state, user) {
      state.user = user;
      state.userRole = user.role;
      Cookies.set('userRole', user.role);
    },
    CLEAR_USER(state) {
      state.user = null;
      state.token = '';
      state.userRole = 'guest';
      Cookies.remove('token');
      Cookies.remove('userRole');
    },
    BUMP_HOME_REFRESH(state) {
      state.homeRefreshKey += 1;
    },
  },
  actions: {
    login({ commit }, userInfo) {
      return new Promise((resolve) => {
        commit('SET_TOKEN', userInfo.token);
        commit('SET_USER', userInfo.user);
        resolve();
      });
    },
    logout({ commit }) {
      commit('CLEAR_USER');
    },
    notifyHomeRefresh({ commit }) {
      commit('BUMP_HOME_REFRESH');
    },
  },
  getters: {
    isAdmin: (state) => state.userRole === 'admin',
    isMember: (state) => state.userRole === 'member',
    isGuest: (state) => state.userRole === 'guest',
    isLoggedIn: (state) => !!state.token,
  },
});
