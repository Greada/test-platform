import api from './index'

export function getLatestBuild() {
    return api.get('/ci/builds/latest')
}

export function getBuildList() {
    return api.get('/ci/builds')
}
