const API_URL = 'http://localhost:8080';

function getToken(){
return localStorage.getItem('token');
}

function saveToken(token){
localStorage.setItem('token', token)
}
function removeToken() {
    localStorage.removeItem('token');
}
async function request(method, url, body = null){
const headers = {
    'Content-Type': 'application/json',
            'Authorization': `Bearer ${getToken()}`
};
     const options = { method, headers };
        if (body) options.body = JSON.stringify(body);

        const response = await fetch(API_URL + url, options);

        if (response.status === 403) {
            removeToken();
            window.location.href = '/index.html';
            return;
        }

        return response;
}