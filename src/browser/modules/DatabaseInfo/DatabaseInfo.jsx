import { connect } from 'react-redux'
import { withBus } from 'preact-suber'
import * as editor from 'shared/modules/editor/editorDuck'
import { LabelItems, RelationshipItems, PropertyItems } from './MetaItems'
import UserDetails from './UserDetails'
import DatabaseKernelInfo from './DatabaseKernelInfo'
import {Drawer, DrawerBody, DrawerHeader} from 'browser-components/drawer'

import styles from './style_meta.css'

export const DatabaseInfo = ({ labels = [], relationshipTypes = [], properties = [], userDetails, databaseKernelInfo, onItemClick }) => {
  return (
    <Drawer id='db-drawer'>
      <DrawerHeader>Database Information</DrawerHeader>
      <DrawerBody>
        <div className={styles.section}>
          <LabelItems labels={labels.map((l) => l.val)} onItemClick={onItemClick} />
        </div>
        <div className={styles.section}>
          <RelationshipItems relationshipTypes={relationshipTypes.map((l) => l.val)} onItemClick={onItemClick} />
        </div>
        <div className={styles.section}>
          <PropertyItems properties={properties.map((l) => l.val)} onItemClick={onItemClick} />
        </div>
        <div className={styles.section}>
          <UserDetails userDetails={userDetails} onItemClick={onItemClick} />
          <DatabaseKernelInfo databaseKernelInfo={databaseKernelInfo} onItemClick={onItemClick} />
        </div>
      </DrawerBody>
    </Drawer>
  )
}

const mapStateToProps = (state) => {
  return state.meta || {}
}
const mapDispatchToProps = (_, ownProps) => {
  return {
    onItemClick: (cmd) => {
      ownProps.bus.send(editor.SET_CONTENT, editor.setContent(cmd))
    }
  }
}

export default withBus(connect(mapStateToProps, mapDispatchToProps)(DatabaseInfo))