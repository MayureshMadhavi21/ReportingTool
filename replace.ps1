Get-ChildItem -Recurse -Filter *.java | ForEach-Object {
     = Get-Content .FullName
     = False
    for ( = 0;  -lt .Count; ++) {
         = []
        
        # Replace private Long id;
        if ( -match '^\s*private Long id;\s*$') {
            [] =  -replace 'private Long id;', 'private String id;'
             = True
        }
        
        # Replace JpaRepository<..., Long>
        if ( -match 'JpaRepository<([^,]+),\s*Long>') {
            [] =  -replace 'JpaRepository<([^,]+),\s*Long>', 'JpaRepository<, String>'
             = True
        }

        # Replace Long templateId, Long queryId in DTOs and Params
        if ( -match 'Long templateId') {
            [] =  -replace 'Long templateId', 'String templateId'
             = True
        }
        if ( -match 'Long queryId') {
            [] =  -replace 'Long queryId', 'String queryId'
             = True
        }

        # Method signatures with Long id
        if ( -match '\(Long id\)') {
            [] =  -replace '\(Long id\)', '(String id)'
             = True
        }
        if ( -match '\(\s*@PathVariable\("id"\)\s*Long\s*id\s*\)') {
            [] =  -replace 'Long\s+id', 'String id'
             = True
        }
        # e.g., getTemplateById(Long id) 
        if ( -match '\bLong\s+id\b') {
            # Let's be careful with this broad replacement, but usually it's method args
            if ( -notmatch 'serialVersionUID') {
                [] =  -replace '\bLong\s+id\b', 'String id'
                 = True
            }
        }
    }
    
    if () {
        Set-Content -Path .FullName -Value 
        Write-Host "Updated "
    }
}
