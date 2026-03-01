const BASE = '/api'

async function request(method, path, body) {
    const res = await fetch(`${BASE}${path}`, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: body != null ? JSON.stringify(body) : undefined,
    })
    if (!res.ok) {
        const err = await res.text()
        throw new Error(err || `HTTP ${res.status}`)
    }
    return res.json()
}

export const api = {
    load:         (code)      => request('POST', '/load',  { code }),
    step:         ()          => request('POST', '/step',  null),
    run:          ()          => request('POST', '/run',   null),
    reset:        ()          => request('POST', '/reset', null),
    input:        (value)     => request('POST', '/input', { value }),
    getState:     ()          => request('GET',  '/state', null),
    scrollStack:  (direction) => request('POST', '/stack/scroll',  { direction }),
    trackMemory:  (address)   => request('POST', '/memory/track',  { address }),
}