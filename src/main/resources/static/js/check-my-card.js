(function () {
	'use strict';

	const form = document.getElementById('check-card-form');
	if (!form) return;

	const input = document.getElementById('card-search');
	const cardId = document.getElementById('selected-card-id');
	const suggestions = document.getElementById('card-suggestions');
	const status = document.getElementById('card-search-status');
	const error = document.getElementById('card-search-error');
	let debounceTimer;
	let activeController;

	function hideSuggestions() {
		suggestions.replaceChildren();
		suggestions.classList.add('d-none');
		input.setAttribute('aria-expanded', 'false');
	}

	function showStatus(message) {
		status.textContent = message;
	}

	function renderSuggestions(cards) {
		suggestions.replaceChildren();
		if (!cards.length) {
			showStatus('Không tìm thấy thẻ phù hợp.');
			hideSuggestions();
			return;
		}

		cards.forEach((card) => {
			const button = document.createElement('button');
			button.type = 'button';
			button.className = 'list-group-item list-group-item-action';
			button.setAttribute('role', 'option');
			button.textContent = card.name + ' · ' + card.cardNumber
				+ ' · ' + card.seriesName + ' · ' + card.language;
			button.addEventListener('click', () => {
				cardId.value = card.id;
				input.value = card.name + ' · ' + card.cardNumber;
				input.classList.remove('is-invalid');
				error.classList.remove('d-block');
				showStatus('');
				hideSuggestions();
			});
			suggestions.appendChild(button);
		});
		suggestions.classList.remove('d-none');
		input.setAttribute('aria-expanded', 'true');
		showStatus('Chọn một thẻ trong danh sách gợi ý.');
	}

	input.addEventListener('input', () => {
		cardId.value = '';
		input.classList.remove('is-invalid');
		error.classList.remove('d-block');
		window.clearTimeout(debounceTimer);
		if (activeController) activeController.abort();
		const keyword = input.value.trim();
		hideSuggestions();

		if (keyword.length < 2) {
			showStatus(keyword ? 'Nhập ít nhất 2 ký tự để tìm thẻ.' : '');
			return;
		}

		showStatus('Đang tìm thẻ...');
		debounceTimer = window.setTimeout(async () => {
			activeController = new AbortController();
			try {
				const params = new URLSearchParams({ keyword: keyword });
				const response = await fetch('/api/cards/suggestions?' + params.toString(), {
					headers: { Accept: 'application/json' },
					credentials: 'same-origin',
					signal: activeController.signal
				});
				if (!response.ok) throw new Error('Không tải được gợi ý thẻ. Vui lòng thử lại.');
				renderSuggestions(await response.json());
			} catch (requestError) {
				if (requestError.name !== 'AbortError') {
					hideSuggestions();
					showStatus(requestError.message || 'Lỗi kết nối khi tìm thẻ.');
				}
			}
		}, 250);
	});

	form.addEventListener('submit', (event) => {
		if (cardId.value) return;
		event.preventDefault();
		input.classList.add('is-invalid');
		error.classList.add('d-block');
		input.focus();
	});

	document.addEventListener('click', (event) => {
		if (!suggestions.contains(event.target) && event.target !== input) hideSuggestions();
	});
})();
