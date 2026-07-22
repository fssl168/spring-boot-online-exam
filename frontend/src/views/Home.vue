<template>
  <page-view :title="false">
    <announcement-list/>
    <banner/>
    <page1/>
  </page-view>
</template>

<script>
import { PageView } from '../layouts'
import Banner from './home/Banner'
import Page1 from './home/Page1'
import AnnouncementList from '../components/Announcement/AnnouncementList'

export default {
  name: 'Home',
  components: {
    PageView,
    Banner,
    Page1,
    AnnouncementList
  }
}
</script>
