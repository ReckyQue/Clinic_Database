import { onBeforeUnmount, onMounted, ref } from 'vue';

const MOBILE_QUERY = '(max-width: 430px)';

export function checkIsMobile() {
  if (typeof window === 'undefined') return false;
  return window.matchMedia(MOBILE_QUERY).matches;
}

export function useMobile() {
  const isMobile = ref(checkIsMobile());
  let media = null;

  const update = () => {
    isMobile.value = !!(media && media.matches);
  };

  onMounted(() => {
    media = window.matchMedia(MOBILE_QUERY);
    update();
    if (media.addEventListener) {
      media.addEventListener('change', update);
    } else {
      media.addListener(update);
    }
  });

  onBeforeUnmount(() => {
    if (!media) return;
    if (media.removeEventListener) {
      media.removeEventListener('change', update);
    } else {
      media.removeListener(update);
    }
  });

  return { isMobile };
}
